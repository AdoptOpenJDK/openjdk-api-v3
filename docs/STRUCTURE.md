# Structure

The adopt api has 2 main components:

1. Updater - Pulls data from a number of sources such as Github and Docker, parses the data and stores it to the DB.
1. Front end -  Serve up reponses to web requests at api.adoptopenjdk.net.


## Updater

The updater periodically polls for new data, and stores that data into the DB. There are 2 types of refresh:
1. Full
    - A full refresh of all data from the various sources
    - Performed when the updater is started and then every 24 hours there after. 
1. Incremental
    - A refresh of only newly added or modified files.
    - Performed every 15 min
 
The sources of data for the api are:

### Github
The binary repositories such as:
 - https://github.com/AdoptOpenJDK/openjdk8-binaries
 - https://github.com/AdoptOpenJDK/openjdk11-binaries 

Each of these repos contains a number of releases, inside each release are a number of assets in general for each asset there is:
- Binary archive (i.e OpenJDK11U-jdk_x64_linux_hotspot_2020-05-02-13-03.tar.gz)
- Metadata file (i.e OpenJDK11U-jdk_x64_linux_hotspot_2020-05-02-13-03.tar.gz.json)
- Checksum (i.e OpenJDK11U-jdk_x64_linux_hotspot_2020-05-02-13-03.tar.gz.sha256.txt)

The updater interacts with the Github api using the [V4 GraphQL interface](https://developer.github.com/v4/guides/intro-to-graphql/).
Once we have obtained the data through the Github api the Upstream (for the upstream OpenJDK project) and Adopt mappers which map the
Github data into the Adopt API models. It does this by iterating through the list of repos and releases, for each binary asset download 
its metadata or checksum and parse their contents in order to extract the data. If metadata is not available then we attempt to extract 
the relevant data by parsing the file name.

In order to speed up access and reduce bandwidth we use Mongo as a cache for this data, when we require data such as the metadata file
or checksum, that data will be provided by the cache (assuming it is present) and asynchronously a refresh of that data will be scheduled
to make sure it is up to date.  

### Docker
The docker repositories are only required for displaying stats. We pull data from the Dockerhub api inside DockerStatsInterface.

### Database
The database stores 3 main types of data:
1. Repos - The binary data extracted from Github
1. Stats - Download statistics
1. HTTP Cache - Cached data used to speed up requests

 
## Frontend

The frontend is a Quarkus application that uses OpenAPI for documentation. Data is polled from the database into memory and requests are then
serviced from that dataset.
