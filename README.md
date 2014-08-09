My solution for the challenge-problem.

After all I came to the right idea of using binary-three for prefixes, but I assumed that every time countActiveUnreachableIPsFromFile or countReachableIPsFromFile are called for different files, and so I have read whole file (which alone takes long time with 1GB file) each time those methods were called. 

Instead I should have calculated both values from the first read for the file, and return those values later without reading file again. Maybe, it would be a good idea, to have an array associated with file name storing those values. Anyway, I'm #6 from 300 students, which isn't that bad. 