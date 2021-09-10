# Search Guard sgctl

![Logo](https://raw.githubusercontent.com/floragunncom/sg-assets/master/logo/sg_dlic_small.png)

`sgctl` is a command line remote control command for Search Guard. It is supposed to be a lightweight and easy-to-use replacement for the older `sgadmin` command.

Currently, `sgctl` is just a tech preview and not complete yet. You can only use it with the Search Guard tech preview version. It cannot be used with older Search Guard release versions.

## Getting Started

In order to use `sgctl`, you need:

- A cluster with Seard Guard installed  
- An admin certificate and the corresponding private key to authenticate at that cluster. The certificate and the private key must be available as PEM files.

To make the initial connection to the cluster, execute the following command:

```
./sgctl.sh connect my-cluster --cert path/to/admin-cert.pem --private-key path/to/private-key.pem
```

If the private key is password protected, specify the paramter `--private-key-pass` in order to get a prompt for the password.

If the cluster uses certificates which are signed by a private PKI, you can specify the root certificate of the PKI by using the parameter `--ca-cert`. 

```
./sgctl.sh connect my-cluster --cert path/to/admin-cert.pem --private-key path/to/private-key.pem --private-key-pass --ca-cert path/to/ca-root-cert.pem
```

If you execute this command, `sgctl` will try to connect to the specified cluster. If the connection is successful, `sgctl` will store the connection configuration locally and re-use it for further commands. This way, you don't have to specify the connection configuration again for each command.


## Usage

### Retrieving Search Guard Configuration

In order to retrieve the current configuration used by Search Guard, you can use the following command:

```
./sgctl.sh get-config -o sg-config
```

This will retrieve the Search Guard configuration and store it locally in a directory called `sg-config`.

### Uploading Search Guard Configuration

In order to upload Search Guard configuration from your local computer, you have several options:

If you only want to upload a single configuration file, use this command:

```
./sgctl.sh upload-config sg-config/sg_internal_users.yml
```

You can also specify the directory to upload all Search Guard configuration files in that directory:

```
./sgctl.sh upload-config sg-config
```

## License

`sgctl` is licensed under the Apache 2 license. See the LICENSE file for details.

Copyright 2016-2021 by floragunn GmbH - All rights reserved

Unless required by applicable law or agreed to in writing, software
distributed here is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

## Support
* Commercial support available through [floragunn GmbH](https://search-guard.com)
* Community support available via [Search Guard Forum](https://forum.search-guard.com)
* Follow us on twitter [@searchguard](https://twitter.com/searchguard)


## Legal
floragunn GmbH is not affiliated with Elasticsearch BV.

Search Guard is a trademark of floragunn GmbH, registered in the U.S. and in other countries.

Elasticsearch, Kibana, Logstash, and Beats are trademarks of Elasticsearch BV, registered in the U.S. and in other countries.
