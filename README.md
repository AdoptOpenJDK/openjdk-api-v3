# AdoptOpenJDK API

**NOTICE:** [AdoptOpenJDK API v1](/README.v1.md) has been deprecated and will be removed.
If you are using v1 please move to the latest version (documented below) as soon as possible.

**NOTICE:** AdoptOpenJDK API v3 is the next version being worked onb, please ensure you pull request to the [v3 branch](https://github.com/AdoptOpenJDK/openjdk-api/tree/v3).

## Overview

The AdoptOpenJDK API provides a way to consume JSON information about the AdoptOpenJDK releases and nightly builds.  Sign up to the [mailing list](https://mail.openjdk.java.net/mailman/listinfo/adoption-discuss) where major API updates will be announced, and visit [adoptopenjdk.net](https://adoptopenjdk.net) to find out more about the community.

## Usage

Here is an example using `curl` (see the [curl documentation](https://curl.haxx.se/docs/tooldocs.html)):

```bash
curl -L 'https://api.adoptopenjdk.net/v2/info/releases/openjdk8'
```

This command returns information about all 'OpenJDK' releases, and defaults to the latest version of the API.

The following [Windows Powershell](https://docs.microsoft.com/en-us/powershell/scripting/getting-started/getting-started-with-windows-powershell?view=powershell-6) script uses `Invoke-Webrequest` to download the latest Windows 64-bit archive.
```
function Get-RedirectedUrl
{
    Param (
        [Parameter(Mandatory=$true)]
        [String]$URL
    )

    $request = [System.Net.WebRequest]::Create($url)
    $request.AllowAutoRedirect=$false
    $response=$request.GetResponse()

    If ($response.StatusCode -eq "Found")
    {
        $response.GetResponseHeader("Location")
    }
}

$url= "https://api.adoptopenjdk.net/v2/binary/nightly/openjdk8?openjdk_impl=hotspot&os=windows&arch=x64&release=latest&type=jdk"

$fUrl = Get-RedirectedUrl $url
$filename = [System.IO.Path]::GetFileName($fUrl);

Write-Host "Downloading $filename"

[Net.ServicePointManager]::SecurityProtocol = "tls12, tls11, tls"
Invoke-WebRequest -Uri $url -OutFile $filename
```

> **Note on the API rate limit:** Add the `-i` option (e.g. `curl -i https://api.adoptopenjdk.net/v2/openjdk8/releases`) to return the response header as well as the response body. There is a limit of 100 API calls per hour per IP, and the value of `X-RateLimit-Remaining` in the response header is useful to determine how many API calls are remaining from this limit.

## API v2.0.0 Specification

You can append different paths to the `https://api.adoptopenjdk.net/v2/` URL, either in the above `curl` format, in a browser, or through an HTTP client, to return different JSON information:

```
/v2/<request type>/<release type>/<version>
```

For instance:

```
/info/latest/openjdk10
curl -L 'https://api.adoptopenjdk.net/v2/info/nightly/openjdk10'
```

### Path Parameters

#### Request Type

##### info

List of information about builds that match the current query

```
curl -L 'https://api.adoptopenjdk.net/v2/info/nightly/openjdk8?openjdk_impl=hotspot'
```

##### binary
Redirects to the binary that matches your current query. If multiple or no binaries match the query, an error code will be returned

```
curl -L 'https://api.adoptopenjdk.net/v2/binary/nightly/openjdk8?openjdk_impl=hotspot&os=windows&arch=x64&release=latest&type=jdk'
```

##### latestAssets
Returns the latest binary asset for every matching combination of os, architecture, binary_type, openjdk_impl, version, heap_size.

i.e to find the latest jdk/jre for linux, x64, normal heap, hotspot:

```
curl -L 'https://api.adoptopenjdk.net/v2/latestAssets/nightly/openjdk8?os=linux&arch=x64&heap_size=normal&openjdk_impl=hotspot'

[
  {
    "os": "linux",
    "architecture": "x64",
    "binary_type": "jre",
    "openjdk_impl": "hotspot",
    "binary_name": "OpenJDK8U-jre_x64_linux_hotspot_2018-10-12-04-26.tar.gz",
    "binary_link": "https://github.com/AdoptOpenJDK/openjdk8-binaries/releases/download/jdk8u-2018-10-12-04-26/OpenJDK8U-jre_x64_linux_hotspot_2018-10-12-04-26.tar.gz",
    "binary_size": 40564422,
    "checksum_link": "https://github.com/AdoptOpenJDK/openjdk8-binaries/releases/download/jdk8u-2018-10-12-04-26/OpenJDK8U-jre_x64_linux_hotspot_2018-10-12-04-26.tar.gz.sha256.txt",
    "version": "8",
    "heap_size": "normal",
    "timestamp": "2018-10-12T04:26:10Z"
  },
  {
    "os": "linux",
    "architecture": "x64",
    "binary_type": "jdk",
    "openjdk_impl": "hotspot",
    "binary_name": "OpenJDK8U-jdk_x64_linux_hotspot_2018-10-12-04-26.tar.gz",
    "binary_link": "https://github.com/AdoptOpenJDK/openjdk8-binaries/releases/download/jdk8u-2018-10-12-04-26/OpenJDK8U-jdk_x64_linux_hotspot_2018-10-12-04-26.tar.gz",
    "binary_size": 78327417,
    "checksum_link": "https://github.com/AdoptOpenJDK/openjdk8-binaries/releases/download/jdk8u-2018-10-12-04-26/OpenJDK8U-jdk_x64_linux_hotspot_2018-10-12-04-26.tar.gz.sha256.txt",
    "version": "8",
    "heap_size": "normal",
    "timestamp": "2018-10-12T04:26:10Z"
  }
]
```

#### Release Type

Type of release, i.e `releases` for stable builds or `nightly` for most recent build.

#### Version

OpenJDK version, i.e `openjdk8`, `openjdk9`, `openjdk10`, `openjdk11`, `openjdk12` 

### Query Parameters

The data that can be returned can then be filtered to find builds of a specific type

| Parameter | Query Parameter Name | Examples |
|-----------|----------------------|----------|
| Open Jdk Implementation | openjdk_impl | hotspot, openj9 |
| Operating System | os | windows, linux, mac |
| Architecture | arch | x64, x32, ppc64, s390x, ppc64le, aarch64 |
| Binary Type | type | jdk, jre |
| Heap Size | heap_size | normal, large |
| Release | release | latest, jdk8u172-b00-201807161800 |

In the absence of a given parameter, it will return all elements. 

To return latest, hotspot, windows, x64, jdk:
```
curl -L 'https://api.adoptopenjdk.net/v2/binary/nightly/openjdk8?openjdk_impl=hotspot&os=windows&arch=x64&release=latest&type=jdk'
```

Multiple values can be supplied for a given parameter like so:
```sh
curl 'https://api.adoptopenjdk.net/v2/info/releases/openjdk8?os=windows&os=linux&arch=x32&arch=x64'
```
This will return all Windows and Linux releases of OpenJDK8 for 32-bit and 64-bit x86 architectures.  
Multi-value `release` queries are not currently supported due to some idiosyncrasies with how it affects response 
formats. Queries such as `?release=latest&release=jdk8u172-b00-201807161800` will return an appropriate error response.
