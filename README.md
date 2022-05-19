# Grype vulnerability scanner

## Introduction

[Grype](https://github.com/anchore/grype) is a vulnerability scanner for container images and filesystems.
This jenkins plugin scans a given target and saves a report as job artifact.


## Getting started
This jenkins plugin installs grype in the job workspace directory and performs scan.
See section [Installation/Recommended](https://github.com/anchore/grype) for more installation details.


#### Grype as a build step:
<img src="images/1.png" alt="Grype plugin" />

<img src="images/2.png" alt="Grype plugin" />

#### Possible scan targets:
<img src="images/3.png" alt="Grype plugin" />

#### Scan result as job artifact:
<img src="images/4.png" alt="Grype plugin" />

#### Scan results:
<img src="images/5.png" alt="Grype plugin" />


### Usage in a pipeline:
```groovy
pipeline
{
  agent any
  options
  {
    skipStagesAfterUnstable()
  }
  stages
  {
    stage('Build')
    {
      steps
      {
        grypeScan scanDest: 'dir:/tmp', repName: 'myScanResult.txt'
      }
    }
  }
}
```

## LICENSE

Licensed under MIT, see [LICENSE](LICENSE.md)

