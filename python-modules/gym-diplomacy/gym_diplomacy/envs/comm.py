# load additional Python module
import socket
import sys
import typing
from threading import Thread

import logging

logging_level = 'DEBUG'
level = getattr(logging, logging_level)
logging.basicConfig(stream=sys.stdout, level=level)
logger = logging.getLogger(__name__)


class LocalSocketServer:
    sock = None
    handle: typing.Callable = None
    threads: typing.List[Thread] = []

    terminate: bool = False

    def __init__(self, port, handle):
        self.handle = handle

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

    def threaded_listen(self):
        thread = Thread(target=self._listen)
        self.threads.append(thread)
        thread.start()

    def _listen(self):
        while not self.terminate:
            # wait for a connection
            logger.debug('waiting for a connection')
            connection, client_address = self.sock.accept()

            try:
                # show who connected to us
                logger.debug('connection from {}'.format(client_address))

                data = connection.recv(1024 * 20)

                logger.info("Calling handler...")
                self.handle(data)

            finally:
                # Clean up the connection
                connection.close()

    def close(self) -> None:
        self.terminate = True
        for thread in self.threads:
            thread.join()


def handle_f(request: bytearray):
    return request


def main_f():
    sock = LocalSocketServer(5000, handle_f)
    sock.listen()


if __name__ == "__main__":
    main_f()
