# jUPnP Tool

jUPnP Tool is a command line tool using jUPnP library.
It will provide options to search and query UPnP devices.

## How to build the tool

Building and running the tool requires Java 17 or newer because it uses the Jetty 12 HTTP transport.
The tool is included automatically when building with Java 17 or newer and is skipped on Java 11.

From this module's directory, build the tool with:

```shell
mvn clean compile assembly:single
```

The executable jar, including its dependencies, is generated at
`target/jupnptool-4.0.0-SNAPSHOT.jar`.

## How to use the tool

From the `target` directory, you can start the tool with:

```shell
$ alias jupnptool="java -jar jupnptool-4.0.0-SNAPSHOT.jar"
$ jupnptool

Usage: jupnptool [options] [command] [command options]
  Options:
    --help, -h, -?
       Help how to use this tool
    --loglevel, -l
       Set LogLevel to {OFF|ERROR|WARN|INFO|DEBUG|TRACE}
       Default: DISABLED
    --multicastResponsePort, -m
       Specify Multicast Response Port
       Default: 0
    --pool, -p
       Configure thread pools and enable pool statistic
       (mainPoolSize,asyncPoolSize[,stats])
       Default: 20,20
    --verbose, -v
       Enable verbose messages
       Default: false
  Commands:
    search      Search for UPnP devices
      Usage: search [options]
        Options:
          --filter, -f
             Filter for devices containing this text (in some description)
             Default: *
          --sort, -s
             Sort using {none|ip|model|serialNumber|manufacturer|udn}
             Default: none
          --timeout, -t
             The timeout when search will be finished
             Default: 10

    info      Show UPnP device information
      Usage: info IP address or UDN

    nop      No operation
      Usage: nop
```

### Thread pool configuration

Use `--pool=mainPoolSize,asyncPoolSize[,stats]` (or `-p`) before the command to set the
maximum number of worker threads in the two jUPnP pools. Both sizes must be positive integers
and default to 20.

- `mainPoolSize` controls the pool shared by registry maintenance and listeners, multicast and
  datagram I/O, stream-server startup, synchronous protocol processing, and outgoing HTTP request tasks.
- `asyncPoolSize` controls the asynchronous protocol pool, including processing received discovery datagrams.
- The optional `stats` argument prints pool and task statistics to standard output during shutdown,
  even when regular logging is disabled.

For example, configure the main pool for up to 32 workers and the asynchronous pool for up to 12,
and print their statistics when the search finishes:

```shell
jupnptool --pool=32,12,stats search --timeout=10
```

These settings are applied when the UPnP service is created. Jetty 12 also manages separate HTTP
client and server I/O thread pools; `--pool` does not change their sizes or include them in the statistics.

### Samples:

```shell
$ jupnptool --help
...
$ jupnptool search
jUPnP Commandline Tool (4.0.0-SNAPSHOT): Search for UPnP devices for 10 seconds sorted by none and filtered by * (poolConfiguration='20,20', multicastResponsePort=0)
IP address       Model                      SerialNumber    
192.168.3.1      FRITZ!Box Fon WLAN 7390    -               
192.168.3.106    QIVICON                    3691234567      
192.168.3.110    NMR                        12345           

$ jupnptool search --timeout=30 --filter=QIVICON
...
$ jupnptool search --sort udn
jUPnP Commandline Tool (4.0.0-SNAPSHOT): Search for UPnP devices for 10 seconds sorted by udn and filtered by * (poolConfiguration='20,20', multicastResponsePort=0)
IP address       Model                      Manufacturer           SerialNumber    UDN                                     
192.168.3.1      FRITZ!Box Fon WLAN 7390    AVM Berlin             -               12345678-1234-40e7-8e6c-246511DDBE2E    
192.168.3.106    QIVICON                    Deutsche Telekom AG    3693BADCC3      861ee488-22b3-11e4-bbea-123456789123    
192.168.3.110    NMR                        Philips                12345           F00DBABE-SA5E-BABA-DADA188ED5A844213     
$ jupnptool --verbose --loglevel=DEBUG search --timeout=60 --filter=Philips
...
```

## Open Issues

* Seldom the ThreadPool is not able to process received packets
TODO here message
* Move build to distribution
