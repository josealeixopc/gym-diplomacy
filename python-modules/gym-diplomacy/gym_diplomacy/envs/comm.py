# load additional Python module
import socket
import sys

import logging

logging_level = 'DEBUG'
level = getattr(logging, logging_level)
logging.basicConfig(stream=sys.stdout, level=level)
logger = logging.getLogger(__name__)


class LocalSocketServer:
    sock = None
    handler = None

    def __init__(self, port, handler):
        self.handler = handler

        # create TCP (SOCK_STREAM) /IP socket
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

        # retrieve local hostname
        local_hostname = socket.gethostname()

        # get fully qualified hostname
        local_fqdn = socket.getfqdn()

        # get the according IP address
        ip_address = socket.gethostbyname(local_hostname)

        # output hostname, domain name and IP address
        logger.debug("Socket working on %s (%s) with %s" % (local_hostname, local_fqdn, ip_address))

        # bind the socket to the port given
        server_address = (ip_address, port)

        logger.debug('Socket starting up on %s port %s' % server_address)
        self.sock.bind(server_address)

        # listen for incoming connections (server mode) with one connection at a time
        self.sock.listen(1)

    def listen(self):
        while True:
            # wait for a connection
            logger.debug('waiting for a connection')
            connection, client_address = self.sock.accept()

            try:
                # show who connected to us
                logger.debug('connection from', client_address)

                byte_array = bytearray()

                # receive the data in small chunks and print it
                while True:
                    data = connection.recv(64)
                    if data:
                        # output received data
                        logger.debug("Data: %s" % data)
                        byte_array.extend(data)

                    else:
                        # no more data -- quit the loop
                        logger.debug("no more data.")
                        break

                    connection.send('Hello. I got something from you.\n'.encode('UTF-8'))

                self.handler.handle(byte_array)

            finally:
                # Clean up the connection
                connection.close()


class RequestHandler:
    def handle(self, request: bytearray):
        logger.debug("RECEIVED MESSAGE")
        logger.debug(request)


# def main_f():
#     handler = RequestHandler()
#     sock = LocalSocketServer(5000, handler)
#     sock.listen()
#
#
# if __name__ == "__main__":
#     main_f()
