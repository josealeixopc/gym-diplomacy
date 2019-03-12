import address_book_pb2
import json

person = address_book_pb2.Person()
person.id = 1
person.name = "John"


print (person.SerializeToString())

person2 = address_book_pb2.Person()
print("Person 2: ", person2)
person2.ParseFromString(person.SerializeToString())
print("Person 2: ", person2.__str__())