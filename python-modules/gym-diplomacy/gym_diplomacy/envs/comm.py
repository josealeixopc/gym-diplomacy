# load additional Python module
import socket
import errno
import sys
import os
import typing
from threading import Thread

import logging

FORMAT = "%(levelname)-8s -- [%(filename)s:%(lineno)s - %(funcName)15s()] %(message)s"
logging.basicConfig(format=FORMAT)

logging_level = 'DEBUG'
level = getattr(logging, logging_level)
logger = logging.getLogger(__name__)
logger.setLevel(level)


class LocalSocketServer:
    handle: typing.Callable = None
    threads: typing.List[Thread] = []

    terminate: bool = False

    def __init__(self, port, handle):
        self.handle = handle

        # retrieve local hostname
        local_hostname = socket.gethostname()

        # get fully qualified hostname
        local_fqdn = socket.getfqdn()

        # get the according IP address
        ip_address = socket.gethostbyname(local_hostname)

        # output hostname, domain name and IP address
        logger.debug("Socket working on %s (%s) with %s" % (local_hostname, local_fqdn, ip_address))

        # bind the socket to the port given
        self.server_address = (ip_address, port)


    def threaded_listen(self):
        thread = Thread(target=self._listen)
        self.threads.append(thread)
        thread.start()


    def _listen(self):
        while not self.terminate:
            # create TCP (SOCK_STREAM) /IP socket
            with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
                # reuse the socket, meaning there should not be any errno98 address already in use
                s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

                logger.debug('Socket starting up on %s port %s' % self.server_address)
                s.bind(self.server_address)

                # listen for incoming connections (server mode) with one connection at a time
                s.listen(1)

                connection, client_address = s.accept()
                with connection:
                    # show who connected to us
                    logger.debug('Connection from {}'.format(client_address))
                    data = connection.recv(1024 * 20)
                    connection.send(self.handle(data))


    def close(self) -> None:
        logger.info("Closing LocalSocketServer...")

        self.terminate = True

        for thread in self.threads:
            thread.join()

        logger.info("LocalSocketServer terminated.")


def handle_f(request: bytearray):
    return request


def main_f():
    sock = LocalSocketServer(5000, handle_f)
    sock.listen()


if __name__ == "__main__":
    main_f()
