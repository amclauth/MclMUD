# MclMUDClient
Simple MUD client in Java

I started this project as a little experiment in seeing how hard it would be to write a MUD client. The
problem with the other clients out there where that they either weren't cross platform (and I am sometimes
on a Mac and other times on a Linux box) or they only seemed to allow scripting through custom variables
and / or languages.

I'd always wondered about building triggers and aliases and command parsing directly into the code of a
client, figuring that might be faster and much more agile (especially in developing a bit of an "AI" for
playing the game). So I decided to try it out.

This is the result (so far). It's a working telnet connection that parses some of the main ansi escape 
sequences in an SWT framework ... mostly the ones for color and text decoration. It can dynamically add 
aliases and connections and has a built in base for handling triggers. Users would have to code their 
triggers in and recompile, but this is Java, it's in an eclipse project, and all in all, that's pretty easy. 
Much easier than finding some of the obscure libraries that many of the other clients need in order to build 
their sources.

So far, I've been using it to play MUME again, and just poking around these design ideas.
