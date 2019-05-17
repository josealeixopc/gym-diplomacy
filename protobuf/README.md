# protobuf

This folder contains the [Protobuf](https://developers.google.com/protocol-buffers/) `.proto` file(s) declarating how messages between the modules of this project are structured and how the correspondent services interact with each other through [gRPC](https://grpc.io/).

The Makefile creates the message and service classes for Python and Java from the Protobuf file.

## Folder structure

The folder structure for the `.proto` file is a bit odd because of an issue with the gRPC plugin. The workaround consist of doing [this](https://github.com/grpc/grpc/issues/9575#issuecomment-293934506).

## Makefile structure

The Makefile runs a `protoc` command, therefore `protoc` must be installed in the machine or a pre-built binary must be used, as described [here](https://github.com/protocolbuffers/protobuf).

The `protoc` command accepts a `--plugin` argument. This argument specififies the type of plugin and the location of the plugin's binary:

`--plugin=<type of plugin>=<path to plugin>`

The `protoc-gen-mypy` script/plugin comes from [mypy-protobuf](https://github.com/dropbox/mypy-protobuf) and creates a `.pyi` file from the Protobuf file, which is useful for IDE autocompletion. It is called as a plugin in the `protoc` invocation. However, it does not generate for the `_grpc` files. Only for files containing messages.

To generate Java gRPC files with plugins, we download the executable for the plugin `protoc-gen-grpc-java` from [here](https://search.maven.org/artifact/io.grpc/protoc-gen-grpc-java/). The repository for the Java plugin is [this one](https://github.com/grpc/grpc-java/).

To generate Python gRPC files with plugins, we had to build the plugin binary as it is not released pre-built. We executed the following steps:

```bash
git clone https://github.com/grpc/grpc      # clone repository
cd grpc
git submodule update --init                 # to include the necessary submodules for build
make grpc_python_plugin                     # build Python plugin from scratch (takes quite a while to build)
```

I believe the binary eventually ends up in the `opt/bin` folder inside the directory (I can't remember). Then, we add it as the `protoc-gen-grpc_python` plugin.

After including this, we must define the output directories **for each language** and **for messages and services** (`--<language>_out` for messages and `--grpc_<language>_out` for services).

## Running

To create/update the classes generated from the Protobuf file:

`make`

To delete the generated classes:

`make clean`