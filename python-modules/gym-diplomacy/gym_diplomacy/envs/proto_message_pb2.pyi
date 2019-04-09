# @generated by generate_proto_mypy_stubs.py.  Do not edit!
import sys
from google.protobuf.descriptor import (
    EnumDescriptor as google___protobuf___descriptor___EnumDescriptor,
)

from google.protobuf.internal.containers import (
    RepeatedCompositeFieldContainer as google___protobuf___internal___containers___RepeatedCompositeFieldContainer,
)

from google.protobuf.message import (
    Message as google___protobuf___message___Message,
)

from typing import (
    Iterable as typing___Iterable,
    List as typing___List,
    Optional as typing___Optional,
    Text as typing___Text,
    Tuple as typing___Tuple,
    cast as typing___cast,
)

from typing_extensions import (
    Literal as typing_extensions___Literal,
)


class BandanaRequest(google___protobuf___message___Message):
    class Type(int):
        DESCRIPTOR: google___protobuf___descriptor___EnumDescriptor = ...
        @classmethod
        def Name(cls, number: int) -> str: ...
        @classmethod
        def Value(cls, name: str) -> BandanaRequest.Type: ...
        @classmethod
        def keys(cls) -> typing___List[str]: ...
        @classmethod
        def values(cls) -> typing___List[BandanaRequest.Type]: ...
        @classmethod
        def items(cls) -> typing___List[typing___Tuple[str, BandanaRequest.Type]]: ...
    INVALID = typing___cast(Type, 0)
    GET_DEAL_REQUEST = typing___cast(Type, 1)
    GET_ACCEPTANCE_REQUEST = typing___cast(Type, 2)
    SEND_GAME_END = typing___cast(Type, 3)

    type = ... # type: BandanaRequest.Type

    @property
    def observation(self) -> ObservationData: ...

    def __init__(self,
        type : typing___Optional[BandanaRequest.Type] = None,
        observation : typing___Optional[ObservationData] = None,
        ) -> None: ...
    @classmethod
    def FromString(cls, s: bytes) -> BandanaRequest: ...
    def MergeFrom(self, other_msg: google___protobuf___message___Message) -> None: ...
    def CopyFrom(self, other_msg: google___protobuf___message___Message) -> None: ...
    if sys.version_info >= (3,):
        def HasField(self, field_name: typing_extensions___Literal[u"observation"]) -> bool: ...
        def ClearField(self, field_name: typing_extensions___Literal[u"observation",u"type"]) -> None: ...
    else:
        def HasField(self, field_name: typing_extensions___Literal[u"observation",b"observation"]) -> bool: ...
        def ClearField(self, field_name: typing_extensions___Literal[b"observation",b"type"]) -> None: ...

class DiplomacyGymResponse(google___protobuf___message___Message):
    class Type(int):
        DESCRIPTOR: google___protobuf___descriptor___EnumDescriptor = ...
        @classmethod
        def Name(cls, number: int) -> str: ...
        @classmethod
        def Value(cls, name: str) -> DiplomacyGymResponse.Type: ...
        @classmethod
        def keys(cls) -> typing___List[str]: ...
        @classmethod
        def values(cls) -> typing___List[DiplomacyGymResponse.Type]: ...
        @classmethod
        def items(cls) -> typing___List[typing___Tuple[str, DiplomacyGymResponse.Type]]: ...
    INVALID = typing___cast(Type, 0)
    CONFIRM = typing___cast(Type, 1)
    SEND_DEAL = typing___cast(Type, 2)
    SEND_ACCEPTANCE = typing___cast(Type, 3)

    type = ... # type: DiplomacyGymResponse.Type

    @property
    def deal(self) -> DealData: ...

    @property
    def acceptance(self) -> AcceptanceData: ...

    def __init__(self,
        type : typing___Optional[DiplomacyGymResponse.Type] = None,
        deal : typing___Optional[DealData] = None,
        acceptance : typing___Optional[AcceptanceData] = None,
        ) -> None: ...
    @classmethod
    def FromString(cls, s: bytes) -> DiplomacyGymResponse: ...
    def MergeFrom(self, other_msg: google___protobuf___message___Message) -> None: ...
    def CopyFrom(self, other_msg: google___protobuf___message___Message) -> None: ...
    if sys.version_info >= (3,):
        def HasField(self, field_name: typing_extensions___Literal[u"acceptance",u"deal"]) -> bool: ...
        def ClearField(self, field_name: typing_extensions___Literal[u"acceptance",u"deal",u"type"]) -> None: ...
    else:
        def HasField(self, field_name: typing_extensions___Literal[u"acceptance",b"acceptance",u"deal",b"deal"]) -> bool: ...
        def ClearField(self, field_name: typing_extensions___Literal[b"acceptance",b"deal",b"type"]) -> None: ...

class ProvinceData(google___protobuf___message___Message):
    id = ... # type: int
    owner = ... # type: int
    sc = ... # type: int

    def __init__(self,
        id : typing___Optional[int] = None,
        owner : typing___Optional[int] = None,
        sc : typing___Optional[int] = None,
        ) -> None: ...
    @classmethod
    def FromString(cls, s: bytes) -> ProvinceData: ...
    def MergeFrom(self, other_msg: google___protobuf___message___Message) -> None: ...
    def CopyFrom(self, other_msg: google___protobuf___message___Message) -> None: ...
    if sys.version_info >= (3,):
        def ClearField(self, field_name: typing_extensions___Literal[u"id",u"owner",u"sc"]) -> None: ...
    else:
        def ClearField(self, field_name: typing_extensions___Literal[b"id",b"owner",b"sc"]) -> None: ...

class ObservationData(google___protobuf___message___Message):
    previousActionReward = ... # type: float
    done = ... # type: bool
    info = ... # type: typing___Text

    @property
    def provinces(self) -> google___protobuf___internal___containers___RepeatedCompositeFieldContainer[ProvinceData]: ...

    def __init__(self,
        provinces : typing___Optional[typing___Iterable[ProvinceData]] = None,
        previousActionReward : typing___Optional[float] = None,
        done : typing___Optional[bool] = None,
        info : typing___Optional[typing___Text] = None,
        ) -> None: ...
    @classmethod
    def FromString(cls, s: bytes) -> ObservationData: ...
    def MergeFrom(self, other_msg: google___protobuf___message___Message) -> None: ...
    def CopyFrom(self, other_msg: google___protobuf___message___Message) -> None: ...
    if sys.version_info >= (3,):
        def ClearField(self, field_name: typing_extensions___Literal[u"done",u"info",u"previousActionReward",u"provinces"]) -> None: ...
    else:
        def ClearField(self, field_name: typing_extensions___Literal[b"done",b"info",b"previousActionReward",b"provinces"]) -> None: ...

class AcceptanceData(google___protobuf___message___Message):

    def __init__(self,
        ) -> None: ...
    @classmethod
    def FromString(cls, s: bytes) -> AcceptanceData: ...
    def MergeFrom(self, other_msg: google___protobuf___message___Message) -> None: ...
    def CopyFrom(self, other_msg: google___protobuf___message___Message) -> None: ...

class DealData(google___protobuf___message___Message):
    class MTOOrderData(google___protobuf___message___Message):
        startProvince = ... # type: int
        destinationProvince = ... # type: int

        def __init__(self,
            startProvince : typing___Optional[int] = None,
            destinationProvince : typing___Optional[int] = None,
            ) -> None: ...
        @classmethod
        def FromString(cls, s: bytes) -> DealData.MTOOrderData: ...
        def MergeFrom(self, other_msg: google___protobuf___message___Message) -> None: ...
        def CopyFrom(self, other_msg: google___protobuf___message___Message) -> None: ...
        if sys.version_info >= (3,):
            def ClearField(self, field_name: typing_extensions___Literal[u"destinationProvince",u"startProvince"]) -> None: ...
        else:
            def ClearField(self, field_name: typing_extensions___Literal[b"destinationProvince",b"startProvince"]) -> None: ...

    powerToPropose = ... # type: int

    @property
    def ourMove(self) -> DealData.MTOOrderData: ...

    @property
    def theirMove(self) -> DealData.MTOOrderData: ...

    def __init__(self,
        powerToPropose : typing___Optional[int] = None,
        ourMove : typing___Optional[DealData.MTOOrderData] = None,
        theirMove : typing___Optional[DealData.MTOOrderData] = None,
        ) -> None: ...
    @classmethod
    def FromString(cls, s: bytes) -> DealData: ...
    def MergeFrom(self, other_msg: google___protobuf___message___Message) -> None: ...
    def CopyFrom(self, other_msg: google___protobuf___message___Message) -> None: ...
    if sys.version_info >= (3,):
        def HasField(self, field_name: typing_extensions___Literal[u"ourMove",u"theirMove"]) -> bool: ...
        def ClearField(self, field_name: typing_extensions___Literal[u"ourMove",u"powerToPropose",u"theirMove"]) -> None: ...
    else:
        def HasField(self, field_name: typing_extensions___Literal[u"ourMove",b"ourMove",u"theirMove",b"theirMove"]) -> bool: ...
        def ClearField(self, field_name: typing_extensions___Literal[b"ourMove",b"powerToPropose",b"theirMove"]) -> None: ...
