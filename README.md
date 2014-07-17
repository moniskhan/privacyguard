SecureInternet
==============
An application which is based on the vpnservice provided by android. With this, you can get all internet traffic in plaintext, even with TLS.


Dependency
==============
[SandroproxyLib](https://code.google.com/p/sandrop/).

Actually I put every required library in the libs directory in the form of jar package. Please just use the jar because I modiffy a little.

Extension
==============
Because it is a project for my thesis, I don't have to much time for this. If you want to do some different analysis on the traffic, you can implement the IPlugin interface in the Plugin directory and then change the class of plugin in MyVpnService.java. If you have any problems about this, feel free to send me an email.
