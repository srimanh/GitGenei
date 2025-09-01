"use client"

import { useState, useCallback, useRef, useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { Upload, X, CheckCircle, AlertCircle, FileText, Folder, Zap, Github, GitBranch } from 'lucide-react'
import Uppy from '@uppy/core'
import XHRUpload from '@uppy/xhr-upload'

interface UploadProgress {
  percentage: number
  uploadedBytes: number
  totalBytes: number
  speed: number
  eta: number
}

interface AnalysisStep {
  id: string
  name: string
  status: 'pending' | 'processing' | 'completed' | 'error'
  description: string
}

interface ProjectUploaderProps {
  onUploadComplete?: (result: any) => void
  onAnalysisComplete?: (analysis: any) => void
}

export function ProjectUploader({ onUploadComplete, onAnalysisComplete }: ProjectUploaderProps) {
  const [isDragging, setIsDragging] = useState(false)
  const [uploadProgress, setUploadProgress] = useState<UploadProgress | null>(null)
  const [isUploading, setIsUploading] = useState(false)
  const [isAnalyzing, setIsAnalyzing] = useState(false)
  const [uploadedFile, setUploadedFile] = useState<File | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [analysisSteps, setAnalysisSteps] = useState<AnalysisStep[]>([])
  const [currentFileId, setCurrentFileId] = useState<string | null>(null)

  const fileInputRef = useRef<HTMLInputElement>(null)
  const uppyRef = useRef<Uppy | null>(null)
  const pollingIntervalRef = useRef<NodeJS.Timeout | null>(null)

  useEffect(() => {
    // Initialize Uppy
    uppyRef.current = new Uppy({
      restrictions: {
        maxFileSize: 5 * 1024 * 1024 * 1024, // 5GB
        allowedFileTypes: ['.zip', '.tar', '.tar.gz', '.rar', '.7z'],
      },
      autoProceed: false,
    })

    uppyRef.current.use(XHRUpload, {
      endpoint: 'http://localhost:8080/api/upload/project',
      method: 'POST',
      formData: true,
      fieldName: 'file',
      headers: {
        'Accept': 'application/json',
      },
      withCredentials: true,
      limit: 1,
      bundle: false,
    })

    // Upload progress
    uppyRef.current.on('upload-progress', (file, progress) => {
      setUploadProgress({
        percentage: Math.round((progress.bytesUploaded / progress.bytesTotal) * 100),
        uploadedBytes: progress.bytesUploaded,
        totalBytes: progress.bytesTotal,
        speed: 0, // Will be calculated
        eta: 0, // Will be calculated
      })
    })

    // Upload success
    uppyRef.current.on('upload-success', (file, response) => {
      setIsUploading(false)
      setUploadProgress(null)
      onUploadComplete?.(response.body)
      if (response.body?.fileId) {
        startAnalysis(response.body.fileId)
      } else {
        setError('Upload succeeded but no file ID received')
      }
    })

    // Upload error
    uppyRef.current.on('upload-error', (file, error, response) => {
      setIsUploading(false)
      setUploadProgress(null)
      setError(`Upload failed: ${error.message}`)
    })

    return () => {
      uppyRef.current?.destroy()
      if (pollingIntervalRef.current) {
        clearInterval(pollingIntervalRef.current)
      }
    }
  }, [onUploadComplete])

  const startProgressPolling = (fileId: string) => {
    console.log('📊 Starting progress polling for fileId:', fileId)

    const pollProgress = async () => {
      try {
        const response = await fetch(`http://localhost:8080/api/upload/status/${fileId}`, {
          credentials: 'include',
        })

        if (response.ok) {
          const projectData = await response.json()
          console.log('📊 Progress poll result:', projectData)

          if (projectData.status) {
            updateAnalysisProgress({
              fileId,
              status: projectData.status,
              message: projectData.statusMessage || getStatusMessage(projectData.status),
              progress: projectData.analysisProgress || 0,
              data: projectData
            })

            // Stop polling if completed or failed
            if (projectData.status === 'COMPLETED' || projectData.status === 'FAILED') {
              if (pollingIntervalRef.current) {
                clearInterval(pollingIntervalRef.current)
                pollingIntervalRef.current = null
              }
            }
          }
        }
      } catch (error) {
        console.error('❌ Progress polling error:', error)
      }
    }

    // Poll every 2 seconds
    pollingIntervalRef.current = setInterval(pollProgress, 2000)

    // Initial poll
    pollProgress()
  }

  const getStatusMessage = (status: string): string => {
    const messages: { [key: string]: string } = {
      'UPLOADED': 'File uploaded successfully',
      'EXTRACTING': 'Extracting project files...',
      'SECURITY_SCANNING': 'Security scan disabled for MVP',
      'ANALYZING': 'AI is analyzing your project...',
      'ORGANIZING': 'Organizing project structure...',
      'CREATING_REPO': 'Creating GitHub repository...',
      'PUSHING_TO_GITHUB': 'Pushing to GitHub...',
      'COMPLETED': 'Analysis completed successfully!',
      'FAILED': 'Analysis failed'
    }
    return messages[status] || `Status: ${status}`
  }

  const updateAnalysisProgress = (progressData: any) => {
    const { status, message, progress } = progressData

    // Map backend status to frontend steps with order
    const statusOrder = [
      'EXTRACTING',
      'SECURITY_SCANNING',
      'ANALYZING',
      'ORGANIZING',
      'CREATING_REPO',
      'PUSHING_TO_GITHUB',
      'COMPLETED'
    ]

    const statusMap: { [key: string]: string } = {
      'EXTRACTING': 'extract',
      'SECURITY_SCANNING': 'scan',
      'ANALYZING': 'analyze',
      'ORGANIZING': 'organize',
      'CREATING_REPO': 'github',
      'PUSHING_TO_GITHUB': 'github',
      'COMPLETED': 'completed'
    }

    const currentStatusIndex = statusOrder.indexOf(status)
    const stepId = statusMap[status]

    setAnalysisSteps(prev => prev.map(step => {
      const stepStatusIndex = statusOrder.findIndex(s => statusMap[s] === step.id)

      if (step.id === stepId) {
        // Current step is processing
        return {
          ...step,
          status: status === 'COMPLETED' ? 'completed' : 'processing',
          description: message
        }
      } else if (stepStatusIndex < currentStatusIndex) {
        // Previous steps should be completed
        return { ...step, status: 'completed' }
      } else {
        // Future steps remain pending
        return { ...step, status: 'pending' }
      }
    }))

    // Handle completion
    if (status === 'COMPLETED') {
      console.log('🎉 Analysis completed! Updating UI...')
      setAnalysisSteps(prev => prev.map(step => ({ ...step, status: 'completed' })))
      setIsAnalyzing(false)

      // Clear polling interval
      if (pollingIntervalRef.current) {
        clearInterval(pollingIntervalRef.current)
        pollingIntervalRef.current = null
      }

      onAnalysisComplete?.({
        fileId: progressData.fileId,
        message: '🎉 SUCCESS! Your project has been pushed to GitHub with organized branches!',
        realAnalysis: true,
        data: progressData.data,
        repositoryUrl: progressData.data?.repositoryUrl,
        branches: progressData.data?.branches || []
      })
    } else if (status === 'FAILED') {
      console.log('❌ Analysis failed:', message)
      setAnalysisSteps(prev => prev.map(step =>
        step.status === 'processing' ? { ...step, status: 'error' } : step
      ))
      setIsAnalyzing(false)

      // Clear polling interval
      if (pollingIntervalRef.current) {
        clearInterval(pollingIntervalRef.current)
        pollingIntervalRef.current = null
      }

      setError(message || 'Analysis failed')
    }
  }

  const startAnalysis = async (fileId: string) => {
    setIsAnalyzing(true)
    setCurrentFileId(fileId)
    setAnalysisSteps([
      { id: 'extract', name: 'Extracting Files', status: 'pending', description: 'Unpacking your project files...' },
      { id: 'scan', name: 'Security Scan', status: 'pending', description: 'Checking for security issues...' },
      { id: 'analyze', name: 'AI Analysis', status: 'pending', description: 'Understanding project structure...' },
      { id: 'organize', name: 'Auto-Organize', status: 'pending', description: 'Creating optimal branch structure...' },
      { id: 'github', name: 'GitHub Setup', status: 'pending', description: 'Creating GitHub repository and pushing...' },
    ])

    try {
      console.log('🚀 Starting real analysis for fileId:', fileId)

      // Call the real backend analysis endpoint
      const response = await fetch(`http://localhost:8080/api/upload/analyze/${fileId}`, {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
        },
      })

      if (!response.ok) {
        throw new Error(`Analysis request failed: ${response.statusText}`)
      }

      const result = await response.json()
      console.log('✅ Analysis started successfully:', result)

      // Start polling for progress updates
      startProgressPolling(fileId)

    } catch (error) {
      console.error('❌ Analysis failed:', error)
      setIsAnalyzing(false)
      setError(`Analysis failed: ${error instanceof Error ? error.message : 'Unknown error'}`)
    }
  }

  const simulateAnalysisStep = async (stepId: string, duration: number) => {
    setAnalysisSteps(prev => prev.map(step => 
      step.id === stepId ? { ...step, status: 'processing' } : step
    ))
    
    await new Promise(resolve => setTimeout(resolve, duration))
    
    setAnalysisSteps(prev => prev.map(step => 
      step.id === stepId ? { ...step, status: 'completed' } : step
    ))
  }

  const handleDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    setIsDragging(true)
  }, [])

  const handleDragLeave = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    setIsDragging(false)
  }, [])

  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    setIsDragging(false)
    
    const files = Array.from(e.dataTransfer.files)
    if (files.length > 0) {
      handleFileSelect(files[0])
    }
  }, [])

  const handleFileSelect = (file: File) => {
    setError(null)
    setUploadedFile(file)
    
    if (uppyRef.current) {
      uppyRef.current.addFile({
        name: file.name,
        type: file.type,
        data: file,
        source: 'Local',
        isRemote: false,
      })
      
      setIsUploading(true)
      uppyRef.current.upload()
    }
  }

  const handleFileInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files
    if (files && files.length > 0) {
      handleFileSelect(files[0])
    }
  }

  const formatBytes = (bytes: number) => {
    if (bytes === 0) return '0 Bytes'
    const k = 1024
    const sizes = ['Bytes', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
  }

  const resetUpload = () => {
    setUploadedFile(null)
    setUploadProgress(null)
    setIsUploading(false)
    setIsAnalyzing(false)
    setError(null)
    setAnalysisSteps([])
    setCurrentFileId(null)
    uppyRef.current?.cancelAll()

    // Clear polling interval if running
    if (pollingIntervalRef.current) {
      clearInterval(pollingIntervalRef.current)
      pollingIntervalRef.current = null
    }
  }

  return (
    <div className="space-y-4">
      {/* Main Upload Area */}
      <motion.div
        className={`relative overflow-hidden rounded-xl border-2 border-dashed transition-all duration-500 ${
          isDragging
            ? "border-cyan-400 bg-cyan-400/10"
            : error
            ? "border-red-400/50 bg-red-400/5"
            : "border-white/20 hover:border-cyan-400/50 bg-white/5"
        } p-12 text-center cursor-pointer group`}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        onClick={() => !isUploading && !isAnalyzing && fileInputRef.current?.click()}
        whileHover={{ scale: 1.02 }}
        transition={{ type: "spring", stiffness: 300, damping: 20 }}
      >
        <input
          ref={fileInputRef}
          type="file"
          accept=".zip,.tar,.tar.gz,.rar,.7z"
          onChange={handleFileInputChange}
          className="hidden"
          disabled={isUploading || isAnalyzing}
        />

        <AnimatePresence mode="wait">
          {error ? (
            <motion.div
              key="error"
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -20 }}
              className="flex flex-col items-center gap-4"
            >
              <AlertCircle className="w-12 h-12 text-red-400" />
              <div>
                <h3 className="text-lg font-semibold text-red-400 mb-2">Upload Failed</h3>
                <p className="text-white/60 text-sm mb-4">{error}</p>
                <button
                  onClick={(e) => {
                    e.stopPropagation()
                    resetUpload()
                  }}
                  className="px-4 py-2 bg-red-500/20 text-red-400 rounded-lg hover:bg-red-500/30 transition-colors"
                >
                  Try Again
                </button>
              </div>
            </motion.div>
          ) : isUploading && uploadProgress ? (
            <motion.div
              key="uploading"
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -20 }}
              className="flex flex-col items-center gap-4"
            >
              <Upload className="w-12 h-12 text-cyan-400 animate-pulse" />
              <div className="w-full max-w-md">
                <div className="flex justify-between text-sm text-white/70 mb-2">
                  <span>Uploading {uploadedFile?.name}</span>
                  <span>{uploadProgress.percentage}%</span>
                </div>
                <div className="w-full bg-white/10 rounded-full h-2">
                  <motion.div
                    className="bg-gradient-to-r from-cyan-400 to-purple-400 h-2 rounded-full"
                    initial={{ width: 0 }}
                    animate={{ width: `${uploadProgress.percentage}%` }}
                    transition={{ duration: 0.3 }}
                  />
                </div>
                <div className="flex justify-between text-xs text-white/50 mt-1">
                  <span>{formatBytes(uploadProgress.uploadedBytes)} / {formatBytes(uploadProgress.totalBytes)}</span>
                </div>
              </div>
            </motion.div>
          ) : isAnalyzing ? (
            <motion.div
              key="analyzing"
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -20 }}
              className="flex flex-col items-center gap-6"
            >
              <motion.div
                animate={{ rotate: 360 }}
                transition={{ duration: 2, repeat: Infinity, ease: "linear" }}
              >
                <Zap className="w-12 h-12 text-purple-400" />
              </motion.div>
              <div className="w-full max-w-md space-y-3">
                <h3 className="text-lg font-semibold text-white mb-4">AI is analyzing your project...</h3>
                {analysisSteps.map((step) => (
                  <div key={step.id} className="flex items-center gap-3 p-3 bg-white/5 rounded-lg">
                    <div className={`w-6 h-6 rounded-full flex items-center justify-center ${
                      step.status === 'completed' ? 'bg-green-500/20 text-green-400' :
                      step.status === 'processing' ? 'bg-purple-500/20 text-purple-400' :
                      'bg-white/10 text-white/40'
                    }`}>
                      {step.status === 'completed' ? (
                        <CheckCircle className="w-4 h-4" />
                      ) : step.status === 'processing' ? (
                        <motion.div
                          animate={{ rotate: 360 }}
                          transition={{ duration: 1, repeat: Infinity, ease: "linear" }}
                          className="w-3 h-3 border-2 border-purple-400 border-t-transparent rounded-full"
                        />
                      ) : (
                        <div className="w-2 h-2 bg-current rounded-full" />
                      )}
                    </div>
                    <div className="flex-1">
                      <div className="font-medium text-white text-sm">{step.name}</div>
                      <div className="text-xs text-white/60">{step.description}</div>
                    </div>
                  </div>
                ))}
              </div>
            </motion.div>
          ) : (
            <motion.div
              key="upload"
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -20 }}
              className="flex flex-col items-center gap-4"
            >
              <div className="relative">
                <Upload className="w-12 h-12 text-cyan-400 group-hover:text-cyan-300 transition-colors" />
                {isDragging && (
                  <motion.div
                    className="absolute inset-0 bg-cyan-400/20 rounded-full blur-lg"
                    animate={{ scale: [1, 1.5, 1] }}
                    transition={{ duration: 1, repeat: Infinity }}
                  />
                )}
              </div>
              <div>
                <h3 className="text-lg font-semibold text-white mb-2">
                  {isDragging ? "Drop your project here!" : "Drag & Drop or Click to Upload"}
                </h3>
                <p className="text-white/60 text-sm mb-2">
                  Upload entire projects up to 5 GB • AI will analyze and organize everything
                </p>
                <div className="flex items-center justify-center gap-4 text-xs text-white/50">
                  <span className="flex items-center gap-1">
                    <Zap className="w-3 h-3" />
                    AI Analysis
                  </span>
                  <span className="flex items-center gap-1">
                    <GitBranch className="w-3 h-3" />
                    Auto Branches
                  </span>
                  <span className="flex items-center gap-1">
                    <Github className="w-3 h-3" />
                    GitHub Push
                  </span>
                </div>
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </motion.div>

      {/* File Info */}
      {uploadedFile && !error && (
        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          className="flex items-center justify-between p-3 bg-white/5 rounded-lg"
        >
          <div className="flex items-center gap-3">
            <FileText className="w-5 h-5 text-cyan-400" />
            <div>
              <div className="font-medium text-white text-sm">{uploadedFile.name}</div>
              <div className="text-xs text-white/60">{formatBytes(uploadedFile.size)}</div>
            </div>
          </div>
          {!isUploading && !isAnalyzing && (
            <button
              onClick={resetUpload}
              className="p-1 hover:bg-white/10 rounded transition-colors"
            >
              <X className="w-4 h-4 text-white/60" />
            </button>
          )}
        </motion.div>
      )}
    </div>
  )
}
