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


class MyTCPHandler(socketserver.BaseRequestHandler):
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

        response: bytes = self.data.upper()
        logger.info('Sending response: {}'.format(response))
        self.request.sendall(response.upper())

# load additional Python module
# import socket
# import errno
# import sys
# import os
# import typing
# from threading import Thread
#
# import logging
#
# FORMAT = "%(levelname)s -- [%(filename)s:%(lineno)s - %(funcName)s()] %(message)s"
# logging.basicConfig(format=FORMAT)
#
# logging_level = 'DEBUG'
# level = getattr(logging, logging_level)
# logger = logging.getLogger(__name__)
# logger.setLevel(level)
# logger.disabled = False
#
#
# class LocalSocketServer:
#     sock = None
#     handle: typing.Callable = None
#     threads: typing.List[Thread] = []
#     port: int
#
#     terminate: bool = False
#
#     def __init__(self, port, handle):
#         self.port = port
#         self.handle = handle
#
#         # create TCP (SOCK_STREAM) /IP socket
#         self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
#
#         # reuse the socket, meaning there should not be any errno98 address already in use
#         # self.sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
#
#         # retrieve local hostname
#         local_hostname = socket.gethostname()
#
#         # get fully qualified hostname
#         local_fqdn = socket.getfqdn()
#
#         # get the according IP address
#         ip_address = socket.gethostbyname(local_hostname)
#
#         # output hostname, domain name and IP address
#         logger.debug("Socket working on %s (%s) with %s" % (local_hostname, local_fqdn, ip_address))
#
#         # bind the socket to the port given
#         server_address = (ip_address, port)
#
#         logger.debug('Socket starting up on %s port %s' % server_address)
#
#         bound: bool = False
#
#         while not bound:
#             try:
#                 logger.debug('Trying to bind to port...')
#                 self.sock.bind(server_address)
#                 bound = True
#                 logger.debug('Binding successful.')
#             except socket.error as e:
#                 if e.errno == errno.EADDRINUSE:
#                     logger.debug('Could not bind to port. Address already in use. Killing processes listening to port.')
#                     # Kill processes listening to port
#                     kill_command = 'kill -9 $(lsof -t -i:{})'.format(port)
#                     os.system(kill_command)
#                 else:
#                     raise e
#
#         # listen for incoming connections (server mode) with one connection at a time
#         self.sock.listen(1)
#
#     def threaded_listen(self):
#         thread = Thread(target=self._listen)
#         self.threads.append(thread)
#         thread.start()
#
#     def _listen(self):
#         while not self.terminate:
#             # wait for a connection
#             logger.debug('Waiting for a connection...')
#             connection, client_address = self.sock.accept()
#
#             try:
#                 # show who connected to us
#                 logger.debug('Connection from {}'.format(client_address))
#
#                 data = connection.recv(1024 * 20)
#
#                 logger.debug("Calling handler...")
#                 connection.send(self.handle(data))
#
#             finally:
#                 # Clean up the connection
#                 logger.debug("Connection closed")
#                 connection.close()
#
#     def close(self) -> None:
#         logger.info("Closing LocalSocketServer...")
#
#         self.terminate = True
#         self.sock.shutdown(socket.SHUT_RDWR)  # further sends and receives are disallowed
#         self.sock.close()
#
#         for thread in self.threads:
#             thread.join()
#
#         logger.info("LocalSocketServer terminated.")
#
#
# def handle_f(request: bytearray):
#     return request
#
#
# def main_f():
#     sock = LocalSocketServer(5000, handle_f)
#     sock.listen()
#
#
# if __name__ == "__main__":
#     main_f()
