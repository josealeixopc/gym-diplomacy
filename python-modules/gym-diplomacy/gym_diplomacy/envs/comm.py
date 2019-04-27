import typing
import logging

import socketserver
import threading

FORMAT = "%(levelname)s -- [%(filename)s:%(lineno)s - %(funcName)s()] %(message)s"
logging.basicConfig(format=FORMAT)

logging_level = 'DEBUG'
level = getattr(logging, logging_level)
logger = logging.getLogger(__name__)
logger.setLevel(level)
logger.disabled = False


class DiplomacyThreadedTCPServer(socketserver.ThreadingMixIn, socketserver.TCPServer):
    port: int
    host: str
    # typing.Type[A] means any subclass of A
    handler: typing.Type[socketserver.BaseRequestHandler]

    def __init__(self, port: int, request_handler_class: typing.Type[socketserver.BaseRequestHandler],
                 host='localhost'):
        self.port = port
        self.host = host
        self.address = (self.host, self.port)
        self.handler = request_handler_class

        # bind_and_activate means that the constructor will bind and activate the server right away
        # Because we want to set up the reuse address flag, this should not be the case, and we bind and activate
        # manually
        super(DiplomacyThreadedTCPServer, self).__init__(self.address, self.handler, bind_and_activate=False)
        self.allow_reuse_address = True
        self.server_bind()
        self.server_activate()
        logger.info("Server bound and activate at address: {}".format(self.server_address))


class DiplomacyTCPHandler(socketserver.BaseRequestHandler):
    """
    The request handler class for our server.

    It is instantiated once per connection to the server, and must
    override the handle() method to implement communication to the
    client.
    """

    data: bytes

    def handle(self):
        self.data = self.request.recv(1024).strip()
        logger.info('Received data: {}'.format(self.data))

        response: bytes = self.server.diplomacy_env.handle(bytearray(self.data))
        logger.info('Sending response: {}'.format(response))
        self.request.sendall(response)



