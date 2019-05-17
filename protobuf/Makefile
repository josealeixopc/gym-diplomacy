#
# Makefile to call protocol buffer compiler for Java/Python
#

CC          :=protoc
SRC         :=gym_diplomacy/envs/proto_message/proto_message.proto

# The SRC is a bit odd, because of this: https://github.com/grpc/grpc/issues/9575#issuecomment-293934506, which results in the PYTHON_WEIRD_RELATIVE_DEST_DIR

PYTHON_NAME :=proto_message_pb2.py
PYTHON_STUB_NAME :=proto_message_pb2.pyi
PYTHON_GRPC_NAME :=proto_message_pb2_grpc.py
PYTHON_WEIRD_GIMMICK := 

JAVA_NAME :=ProtoMessage.java

# The rest of the path comes from the path of the proto file (SRC). This is used for the protoc command
PYTHON_WEIRD_RELATIVE_DEST_DIR := ../python-modules/gym-diplomacy	

JAVA_RELATIVE_DEST_DIR   := ../java-modules/bandana/src/main/java

# This is the true python destination dir
PYTHON_TRUE_RELATIVE_DEST_DIR := ../python-modules/gym-diplomacy/envs/proto_message

JAVA_PACKAGE := cruz/agents

PYTHON_OUT		:= $(PYTHON_TRUE_RELATIVE_DEST_DIR)/$(PYTHON_NAME)
PYTHON_STUB_OUT := $(PYTHON_TRUE_RELATIVE_DEST_DIR)/$(PYTHON_STUB_NAME)
PYTHON_GRPC_OUT := $(PYTHON_TRUE_RELATIVE_DEST_DIR)/$(PYTHON_GRPC_NAME)

JAVA_OUT        := $(JAVA_RELATIVE_DEST_DIR)/$(JAVA_PACKAGE)/$(JAVA_NAME)
JAVA_GRPC_OUT_FILES := $(wildcard $(JAVA_RELATIVE_DEST_DIR)/$(JAVA_PACKAGE)/*Grpc.java)	# This may represent MORE THAN ONE file

all: $(PYTHON_OUT) $(JAVA_OUT)

$(PYTHON_OUT) $(PYTHON_STUB_OUT) $(PYTHON_GRPC_OUT): $(SRC)
	$(CC) $(SRC) --plugin=protoc-gen-mypy=./protoc-gen-mypy \
	--plugin=protoc-gen-grpc_python=./grpc_python_plugin \
	--grpc_python_out=$(PYTHON_WEIRD_RELATIVE_DEST_DIR) \
	--python_out=$(PYTHON_WEIRD_RELATIVE_DEST_DIR) \
	--mypy_out=$(PYTHON_WEIRD_RELATIVE_DEST_DIR)

$(JAVA_OUT) $(JAVA_GRPC_OUT_FILES): $(SRC)
	$(CC) $(SRC) --plugin=protoc-gen-grpc-java=./protoc-gen-grpc-java-1.20.0-linux-x86_64.exe \
	--grpc-java_out=$(JAVA_RELATIVE_DEST_DIR) \
	--java_out=$(JAVA_RELATIVE_DEST_DIR) 

clean:
	$(RM) $(PYTHON_OUT)
	$(RM) $(PYTHON_STUB_OUT)
	$(RM) $(PYTHON_GRPC_OUT)
	$(RM) $(JAVA_OUT)
	$(RM) $(JAVA_GRPC_OUT_FILES)
