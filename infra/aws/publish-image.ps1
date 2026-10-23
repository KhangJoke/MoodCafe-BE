param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[a-zA-Z0-9_][a-zA-Z0-9_.-]{0,127}$')]
    [string]$Tag
)
$ErrorActionPreference = 'Stop'
$infraPath = $PSScriptRoot
$repositoryPath = [IO.Path]::GetFullPath((Join-Path $infraPath '../..'))
$region = & terraform "-chdir=$infraPath" output -raw aws_region
if ($LASTEXITCODE -ne 0) { throw 'Terraform outputs unavailable. Apply infrastructure first.' }
$imageRepository = & terraform "-chdir=$infraPath" output -raw ecr_repository_url
if ($LASTEXITCODE -ne 0) { throw 'Cannot read ECR repository output.' }
$registry = ($imageRepository -split '/')[0]
$registryPassword = & aws ecr get-login-password --region $region
if ($LASTEXITCODE -ne 0) { throw 'AWS ECR authentication failed.' }
try {
    $registryPassword | & docker login --username AWS --password-stdin $registry
    if ($LASTEXITCODE -ne 0) { throw 'Docker registry login failed.' }
    & docker buildx build --platform linux/arm64 --push `
        -f (Join-Path $infraPath 'Dockerfile') `
        -t "${imageRepository}:$Tag" (Join-Path $repositoryPath 'moodcafe-be')
    if ($LASTEXITCODE -ne 0) { throw 'ARM64 build or push failed.' }
} finally {
    $registryPassword = $null
    & docker logout $registry | Out-Null
}
Write-Host "Image published. On EC2 run: sudo /opt/moodcafe/deploy.sh $Tag"

