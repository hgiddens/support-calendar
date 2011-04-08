# support-calendar

Generates iCalendar files from the NZRB support roster.

## Usage

Running the server is as simple as executing the jar file:

    % java -jar support-calendar.jar

If accessing the support roster requires authentication, then specify
appropriate values for the `jcifs.smb.client.domain`,
`jcifs.smb.client.username`, and `jcifs.smb.client.password` system
properties.

## How to build

Use [Leiningen](http://github.com/technomancy/leiningen) to build the jar file:

    % lein uberjar

The above command will create two `.jar` files. The “standalone” jar is the
one that should be deployed.

## License

Copyright © 2011 Hugh Giddens

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the “Software”), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
