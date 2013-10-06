DIY-Remote
==========

Simple android application and server for remote PC control

Server running
--------------

Server loads scripts from `~/.diy-remote/scripts`

CLI arguments:
* help - print help
* port - port for web server, 9090 by default

Usage: 
```
> diy-remote -port 9595
```

Scripts
-------

Server uses simple groovy scripts which returns map in form:
```
[
  name: 'script_name',
  description: 'Script description',
  icon: 'icon_on_client',
  order: 7, // number for sort actions
  action: {
      // script code here
  }
]
```

Android application
-------------------

Android client have only one setting - URL of web sever with port in form `http://host:port`

VLC integration
---------------

Set of scripts for VLC remote control

Requires: 
 * Configured HTTP interface in VLC
 * Default password is `admin` (can be changed in scripts)

Actions:
* Play
* Pause
* Stop
* Next
* Previous
* Volume up
* Volume down
* Fullscreen

Development
-----------

We recommend to use **Intellij Idea** with **gradle** build tool:
```
> gradle clean assemble idea
```

Contribution
------------

Any ideas and pull requests are welcome!

Good luck and have fun!