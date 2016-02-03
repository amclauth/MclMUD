Config file editing:
	Go ahead! It's not "safe" per se, but it's just a text file. If it's messed
	up, just delete it and it'll be recreated. You can back this up if you want
	to preserve multiple MUD connections. 
	
	To keep any given connection (or to add it), it must be in the list of names
	with the MUDS key and it must have host and port information in its own key.
	For example, if I had two, Diskworld and MUME, my keys would look like this:
	
	MUDS=M.U.M.E.\:Diskworld
	M.U.M.E.=mume.org\:4242
	Diskworld=diskworld.starturtle.net\:4242
	
	Note the "\" escaping the colon in each case. That's the only special character
	this class cares about, though of course, others might be important to avoid
	because the name will be part of a filename, the hostname is a DNS or IP 
	address, and the port should just be an integer.
	
Aliases:
	Add a file with the same name as the name you gave the MUD (or will give it)
	in the connection. For example, if the name is "M.U.M.E.", then add a file
	named "m.u.m.e..alias" (note the all lower case of the file name). If you don't 
	care to do that, add an alias in the game and the file will be created for you.
	
	Once you have the file, aliases can be added two ways:
	
	1) In the game
		Add an alias in the game by prefixing it with "alias" and then the alias
		command, a colon, and a list of commands separated by colons. For example,
		adding an alias to travel in a circle might look like this:
		alias circle:n;e;s;w
		
		Then, when you type this command ("circle") in the input box in this MUD,
		it will execute those four commands in sequence.
		
	2) In the file
		It's almost the same in the file. Just add a new line with your alias in 
		the same format, excluding the word "alias":
		circle:n;e;s;w
		
Triggers, scripts, and formatting
	Triggers, scripts, and formatting are handled a little differently in this 
	client. I wanted to build something where I could actually *code* these
	effects rather than learn or implement a scripting language into existing
	code. 
	
	The downside of this is that you have to actually compile the code if you want
	to add these functions. 
	
	The upside is that they'll be processed quickly (though in their own thread),
	you can code them directly in Java to do *anything*, and you have direct and
	immediate access for your code to the information coming from the MUD.
	
AI Programs
	AI classes should implement the AIInterface, which is explained in the file
	itself. 
	
	A new AI class has to also be added to the AIMap in the AIListener constructor.
	After it's there, the AI should attempt to be used if the name of the MUD is
	the same as the name registered in the AIMap. If you want to have multiple MUD
	names for the same AI, this is possible by simply registering multiple names to
	the same AI. You might want to do this if you wanted to connect to the MUD with
	different aliases and settings, but still use the same AI.
	
	To swap the AI yourself, use the command "#loadAI <name>". In the case above,
	that would be "#loadAI m.u.m.e."