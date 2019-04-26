# Import general packages for testing
import unittest

# Import specific dependencies for test setup
import socketserver
import socket
import threading

# Import packages to be tested
from gym_diplomacy.envs import comm


# class MySocketServerConstructorTestCase(unittest.TestCase):
#     def test_initialize_with_port(self):
#         test_port: int = 5000
#         default_host: str = 'localhost'
#
#         server = comm.MySocketServer(test_port)
#
#         self.assertEqual(server.port, test_port)
#         self.assertEqual(server.host, default_host)
#         self.assertEqual(server.socket_server.server_address, (default_host, test_port))
#
#     def test_initialize_with_port_and_host(self):
#         test_port: int = 5000
#         test_host: str = 'localhost'
#
#         server = comm.MySocketServer(test_port, test_host)
#
#         self.assertEqual(server.port, test_port)
#         self.assertEqual(server.host, test_host)
#         self.assertEqual(server.socket_server.server_address, (test_host, test_port))

class MyTCPHandlerTestCase(unittest.TestCase):
    server_thread: threading.Thread
    host: str = "localhost"
    port: int = 9999

    def setUp(self) -> None:
        # Create the server, binding to localhost on port 9999
        # bind_and_activate determines whether the constructor tries to bind and activate the socket right away
        # because we want to define the allow_reuse_address flag, we set it to false
        self.server = socketserver.TCPServer((self.host, self.port), comm.MyTCPHandler, bind_and_activate=False)
        self.server.allow_reuse_address = True
        self.server.server_bind()
        self.server.server_activate()

        # Activate the server; this will keep running until you
        # interrupt the program with Ctrl-C
        self.server_thread = threading.Thread(target=self.server.serve_forever)
        # Exit the server thread when the main thread terminates
        self.server_thread.daemon = True
        self.server_thread.start()

    def test_handler(self) -> None:
        test_message: bytes = b'Hello, world'

        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
            s.connect((self.host, self.port))
            s.sendall(test_message)
            data = s.recv(1024)

            self.assertEqual(data, test_message.upper())

    def tearDown(self) -> None:
        self.server.shutdown()
        self.server.server_close()
