// Deployment configuration for GitGenei Frontend
// This file will contain deployment-specific settings

export interface DeploymentConfig {
  environment: 'development' | 'staging' | 'production';
  buildCommand: string;
  outputDirectory: string;
  environmentVariables: Record<string, string>;
}

// TODO: Implement deployment configurations
export const deploymentConfigs: Record<string, DeploymentConfig> = {
  development: {
    environment: 'development',
    buildCommand: 'npm run build',
    outputDirectory: '.next',
    environmentVariables: {}
  },
  staging: {
    environment: 'staging',
    buildCommand: 'npm run build',
    outputDirectory: '.next',
    environmentVariables: {}
  },
  production: {
    environment: 'production',
    buildCommand: 'npm run build',
    outputDirectory: '.next',
    environmentVariables: {}
  }
};
