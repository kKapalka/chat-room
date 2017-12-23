Chat-room - application for communication between many users
============================================================
Database info (required for personal use of the app):
database consists of 3 tables: users, messages, mutes
column ordering of rach is as follows:
users - id int, login varchar(30), pass char(32), email varchar(80), verification code char(10), isverified bool
messages - id int, username varchar(30), sendtime timestamp no timezone, message text
mutes - id int, muter varchar(30), muted varchar(30)

As long as tablenames and info ordering follows this convention, application will work regardless of naming of individual columns.

Functionality (as of 23.12.2017):
Register and Verification (email-sending is currently commented out, uncomment if you wish)
Login and Logout using individual usernames
Sending messages

Missing functionality:
Muting of individual users

Documented bugs:
Empty messages throw NullPointerExceptions

Info:
Query text is as of current moment written out in console, for debugging purposes