# OcavaS3

This component provides a number of useful utilities to connect to AWS services including caches to reduce costs 
from repeated connections.

The credentials for S3 access can be provided in one of the following ways:
* As part of the S3Config
* Using the credentials file - `<HOME_DIR>/.ocava_access_credentials/credentials` by providing values for `s3_access_key`, `s3_secret_key` and `s3_endpoint`

Credentials coming through config takes priority over the ones provided through credentials file