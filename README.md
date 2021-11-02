# Search Guard sgctl

`sgctl` is a command line remote control command for Search Guard. It is supposed to be a lightweight and easy-to-use replacement for the older `sgadmin` command.

Currently, `sgctl` is just a tech preview and not complete yet. You can only use it with the Search Guard tech preview version. It cannot be used with older Search Guard release versions.

## Getting Started

In order to use `sgctl`, you need:

- A cluster with Seard Guard installed  
- An admin certificate and the corresponding private key to authenticate at that cluster. The certificate and the private key must be available as PEM files.

To make the initial connection to the cluster, execute the following command:

```shell
./sgctl.sh connect my-cluster --cert path/to/admin-cert.pem --private-key path/to/private-key.pem
```

If the private key is password protected, specify the paramter `--private-key-pass` in order to get a prompt for the password.

If the cluster uses certificates which are signed by a private PKI, you can specify the root certificate of the PKI by using the parameter `--ca-cert`. 

```shell
./sgctl.sh connect my-cluster --cert path/to/admin-cert.pem --private-key path/to/private-key.pem --private-key-pass --ca-cert path/to/ca-root-cert.pem
```

If you execute this command, `sgctl` will try to connect to the specified cluster. If the connection is successful, `sgctl` will store the connection configuration locally and re-use it for further commands. This way, you don't have to specify the connection configuration again for each command.

## Usage

### Retrieving Search Guard Configuration

In order to retrieve the current configuration used by Search Guard, you can use the following command:

```shell
./sgctl.sh get-config -o sg-config
```

This will retrieve the Search Guard configuration and store it locally in a directory called `sg-config`.

### Uploading Search Guard Configuration

In order to upload Search Guard configuration from your local computer, you have several options:

If you only want to upload a single configuration file, use this command:

```shell
./sgctl.sh update-config sg-config/sg_internal_users.yml
```

You can also specify the directory to upload all Search Guard configuration files in that directory:

```
./sgctl.sh update-config sg-config
```

### Migrating legacy Search Guard Configuration

If you want to automatically migrate your legacy Search Guard configuration, you can use the `migrate-config` command:

```shell
./sgctl.sh migrate-config /path/to/legacy/sg-config.yml /path/to/legacy/kibana.yml
```

The command will the display the necessary update instructions. 

If you want to write the new configuration to local files, use the `-o` option:

```shell
./sgctl.sh migrate-config /path/to/legacy/sg-config.yml /path/to/legacy/kibana.yml -o /path/to/outputdir
```

You can choose the type of the target platform using the `--target-platform` option. Valid values are:

- `es`: Elasticsearch up to 7.10.x (default) 
- `es711`: Elasticsearch 7.11.0 and newer
- `os`: OpenSearch 

### Retrieving the Search Guard component state

The Search Guard component state can be useful when debugging issues with a cluster running Search Guard. You can retrieve it using the command.

```shell
./sgctl.sh component-state
```


### User administration

In order to get an internal user, you can use the following command:

```shell
./sgctl.sh get-user userName
```

In order to add a new internal user, you can use the following command:

```shell
./sgctl.sh add-user userName -r sg-role1,sg-role2 --backend-roles backend-role1,backend-role2 -a a=1,b.c.d=2,e=foo --password pass
```

In order to update an internal user, you can use the following command:

```shell
./sgctl.sh update-user userName -r sg-role1,sg-role2 --backend-roles backend-role1,backend-role2 -a a=1,b.c.d=2,e=foo --password pass
```

In order to delete an internal user, you can use the following command:

```shell
./sgctl.sh delete-user userName
```

## License

`sgctl` is licensed under the Apache 2 license. See the LICENSE file for details.

Copyright 2021 by floragunn GmbH - All rights reserved

Unless required by applicable law or agreed to in writing, software
distributed here is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

## Support
* Commercial support available through [floragunn GmbH](https://search-guard.com)
* Community support available via [Search Guard Forum](https://forum.search-guard.com)
* Report issues with `sgctl` at the [floragunn Gitlab repository](https://git.floragunn.com/search-guard/sgctl/-/issues)
* Follow us on twitter [@searchguard](https://twitter.com/searchguard)


## Legal
floragunn GmbH is not affiliated with Elasticsearch BV.

Search Guard is a trademark of floragunn GmbH, registered in the U.S. and in other countries.

Elasticsearch, Kibana, Logstash, and Beats are trademarks of Elasticsearch BV, registered in the U.S. and in other countries.
