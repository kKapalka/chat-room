Chat-room - application for communication between many users
============================================================
Database info (required for personal use of the app):

Changelog #2: 19.01.2018

- Server now needs to log in to its database only once. Data is retained after successful login in a file "dbfile.txt", which is in top-most folder of project directory

Changelog #1: 18.01.2018

- Server can now set up its own database. It's achieved through "Po³¹cz z baz¹ danych" button - it prompts to point at the database to use, and provide the login and pass to PostgreSQL admin.

	IMPORTANT: It creates a new database using the name provided. First-time server users are advised to set up their database this way.

- Password Textfields are now properly hiding the input

-Low-level databas logic is now moved to different class


Functionality (as of 23.12.2017):

Register and Verification (email-sending is currently commented out, uncomment if you wish)

Login and Logout using individual usernames

Sending messages

Muting of individual users


Info:

Query text is as of current moment written out in console, for debugging purposes