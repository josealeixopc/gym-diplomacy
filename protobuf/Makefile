#
# Makefile to call protocol buffer compiler for Java/Python
#

CC          :=protoc
SRC         :=proto_message.proto

PYTHON_NAME  :=proto_message_pb2.py
JAVA_NAME    :=ProtoMessage.java

PYTHON_RELATIVE_DEST_DIR 	:= ../python-modules/gym-diplomacy/gym_diplomacy/envs
JAVA_RELATIVE_DEST_DIR 		:= ../java-modules/bandana/src/main/java

JAVA_PACKAGE	:= cruz/agents

PYTHON_OUT 			:= $(PYTHON_RELATIVE_DEST_DIR)/$(PYTHON_NAME)
JAVA_OUT				:= $(JAVA_RELATIVE_DEST_DIR)/$(JAVA_PACKAGE)/$(JAVA_NAME)

all: $(PYTHON_OUT) $(JAVA_OUT)

$(PYTHON_OUT): $(SRC)
	$(CC) $(SRC) --python_out=$(PYTHON_RELATIVE_DEST_DIR)
$(JAVA_OUT): $(SRC)
	$(CC) $(SRC) --java_out=$(JAVA_RELATIVE_DEST_DIR)

clean:
	$(RM) $(PYTHON_OUT)
	$(RM) $(JAVA_OUT)