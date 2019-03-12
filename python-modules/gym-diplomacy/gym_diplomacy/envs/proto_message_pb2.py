# -*- coding: utf-8 -*-
# Generated by the protocol buffer compiler.  DO NOT EDIT!
# source: proto_message.proto

import sys
_b=sys.version_info[0]<3 and (lambda x:x) or (lambda x:x.encode('latin1'))
from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from google.protobuf import reflection as _reflection
from google.protobuf import symbol_database as _symbol_database
# @@protoc_insertion_point(imports)

_sym_db = _symbol_database.Default()




DESCRIPTOR = _descriptor.FileDescriptor(
  name='proto_message.proto',
  package='dip_q',
  syntax='proto2',
  serialized_options=_b('\n\013cruz.agents'),
  serialized_pb=_b('\n\x13proto_message.proto\x12\x05\x64ip_q\"\x9a\x01\n\x07\x43ommand\x12(\n\x04type\x18\x01 \x01(\x0e\x32\x1a.dip_q.Command.CommandType\x12\x0c\n\x04name\x18\x02 \x01(\t\x12\x1d\n\x04\x64\x61ta\x18\x03 \x03(\x0b\x32\x0f.dip_q.GameData\"8\n\x0b\x43ommandType\x12\x0e\n\nSTART_TEST\x10\x00\x12\x0b\n\x07RESULTS\x10\x01\x12\x0c\n\x08SHUTDOWN\x10\x03\"\x8b\x01\n\tPowerData\x12(\n\x04name\x18\x01 \x01(\x0e\x32\x1a.dip_q.PowerData.PowerName\"T\n\tPowerName\x12\x08\n\x04None\x10\x00\x12\x07\n\x03\x41US\x10\x01\x12\x07\n\x03\x45NG\x10\x02\x12\x07\n\x03\x46RA\x10\x03\x12\x07\n\x03GER\x10\x04\x12\x07\n\x03ITA\x10\x05\x12\x07\n\x03RUS\x10\x06\x12\x07\n\x03TUR\x10\x07\";\n\x0cProvinceData\x12\x1f\n\x05owner\x18\x01 \x01(\x0b\x32\x10.dip_q.PowerData\x12\n\n\x02sc\x18\x02 \x01(\x08\"2\n\x08GameData\x12&\n\tprovinces\x18\x01 \x03(\x0b\x32\x13.dip_q.ProvinceDataB\r\n\x0b\x63ruz.agents')
)



_COMMAND_COMMANDTYPE = _descriptor.EnumDescriptor(
  name='CommandType',
  full_name='dip_q.Command.CommandType',
  filename=None,
  file=DESCRIPTOR,
  values=[
    _descriptor.EnumValueDescriptor(
      name='START_TEST', index=0, number=0,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='RESULTS', index=1, number=1,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='SHUTDOWN', index=2, number=3,
      serialized_options=None,
      type=None),
  ],
  containing_type=None,
  serialized_options=None,
  serialized_start=129,
  serialized_end=185,
)
_sym_db.RegisterEnumDescriptor(_COMMAND_COMMANDTYPE)

_POWERDATA_POWERNAME = _descriptor.EnumDescriptor(
  name='PowerName',
  full_name='dip_q.PowerData.PowerName',
  filename=None,
  file=DESCRIPTOR,
  values=[
    _descriptor.EnumValueDescriptor(
      name='None', index=0, number=0,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='AUS', index=1, number=1,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='ENG', index=2, number=2,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='FRA', index=3, number=3,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='GER', index=4, number=4,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='ITA', index=5, number=5,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='RUS', index=6, number=6,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='TUR', index=7, number=7,
      serialized_options=None,
      type=None),
  ],
  containing_type=None,
  serialized_options=None,
  serialized_start=243,
  serialized_end=327,
)
_sym_db.RegisterEnumDescriptor(_POWERDATA_POWERNAME)


_COMMAND = _descriptor.Descriptor(
  name='Command',
  full_name='dip_q.Command',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='type', full_name='dip_q.Command.type', index=0,
      number=1, type=14, cpp_type=8, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='name', full_name='dip_q.Command.name', index=1,
      number=2, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='data', full_name='dip_q.Command.data', index=2,
      number=3, type=11, cpp_type=10, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
    _COMMAND_COMMANDTYPE,
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto2',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=31,
  serialized_end=185,
)


_POWERDATA = _descriptor.Descriptor(
  name='PowerData',
  full_name='dip_q.PowerData',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='name', full_name='dip_q.PowerData.name', index=0,
      number=1, type=14, cpp_type=8, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
    _POWERDATA_POWERNAME,
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto2',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=188,
  serialized_end=327,
)


_PROVINCEDATA = _descriptor.Descriptor(
  name='ProvinceData',
  full_name='dip_q.ProvinceData',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='owner', full_name='dip_q.ProvinceData.owner', index=0,
      number=1, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='sc', full_name='dip_q.ProvinceData.sc', index=1,
      number=2, type=8, cpp_type=7, label=1,
      has_default_value=False, default_value=False,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto2',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=329,
  serialized_end=388,
)


_GAMEDATA = _descriptor.Descriptor(
  name='GameData',
  full_name='dip_q.GameData',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='provinces', full_name='dip_q.GameData.provinces', index=0,
      number=1, type=11, cpp_type=10, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto2',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=390,
  serialized_end=440,
)

_COMMAND.fields_by_name['type'].enum_type = _COMMAND_COMMANDTYPE
_COMMAND.fields_by_name['data'].message_type = _GAMEDATA
_COMMAND_COMMANDTYPE.containing_type = _COMMAND
_POWERDATA.fields_by_name['name'].enum_type = _POWERDATA_POWERNAME
_POWERDATA_POWERNAME.containing_type = _POWERDATA
_PROVINCEDATA.fields_by_name['owner'].message_type = _POWERDATA
_GAMEDATA.fields_by_name['provinces'].message_type = _PROVINCEDATA
DESCRIPTOR.message_types_by_name['Command'] = _COMMAND
DESCRIPTOR.message_types_by_name['PowerData'] = _POWERDATA
DESCRIPTOR.message_types_by_name['ProvinceData'] = _PROVINCEDATA
DESCRIPTOR.message_types_by_name['GameData'] = _GAMEDATA
_sym_db.RegisterFileDescriptor(DESCRIPTOR)

Command = _reflection.GeneratedProtocolMessageType('Command', (_message.Message,), dict(
  DESCRIPTOR = _COMMAND,
  __module__ = 'proto_message_pb2'
  # @@protoc_insertion_point(class_scope:dip_q.Command)
  ))
_sym_db.RegisterMessage(Command)

PowerData = _reflection.GeneratedProtocolMessageType('PowerData', (_message.Message,), dict(
  DESCRIPTOR = _POWERDATA,
  __module__ = 'proto_message_pb2'
  # @@protoc_insertion_point(class_scope:dip_q.PowerData)
  ))
_sym_db.RegisterMessage(PowerData)

ProvinceData = _reflection.GeneratedProtocolMessageType('ProvinceData', (_message.Message,), dict(
  DESCRIPTOR = _PROVINCEDATA,
  __module__ = 'proto_message_pb2'
  # @@protoc_insertion_point(class_scope:dip_q.ProvinceData)
  ))
_sym_db.RegisterMessage(ProvinceData)

GameData = _reflection.GeneratedProtocolMessageType('GameData', (_message.Message,), dict(
  DESCRIPTOR = _GAMEDATA,
  __module__ = 'proto_message_pb2'
  # @@protoc_insertion_point(class_scope:dip_q.GameData)
  ))
_sym_db.RegisterMessage(GameData)


DESCRIPTOR._options = None
# @@protoc_insertion_point(module_scope)
