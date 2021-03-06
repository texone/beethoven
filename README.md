# Application setup

The application is build using maven look into [build setup](build_setup) for more details. Once you have build the application, you will see a folder beethoven-1.0. Inside you find the following items.

- data folder this contains all the  data for the application
- lib folder this contains all the dependencies
- Watchdog folder contains the watchdog to run the application
- beethoven-1.0.jar the application java archive
- start.bat the batch file to start the application this calls the watchdog this will use watchdog config file to start the application
- watchdog.xml the watchdog configuration to start the app

For the initial setup of the application you have to do the following steps.

Once you have started the app you will see the black animation full screen with alt tab you can bring the settings ui into front.

## Save your settings

To setup the application you have to ensure to save all changes applied to the settings. To do so right click on the `app` pane and the settings dialog appears. Click `update current preset`.

In the appearing dialog choose `self contained` and press `okay`. Now your setting is saved.

## Watchdog Usage

The commandline usage: i.e. with windows executable:

    'watchdog.exe [--configfile <watchdog.xml>] [--copyright] [--help] [--no_restart] [--revision] [--revisions] [--version] watchdog.XML'

The default configfile will be loaded in current working directoy with name 'watchdog.xml', the parameter '--configfile <watchdog.xml>' will use given configfile.
The common watchdog behaviour is to restart application once they exit by any reason, to prevent this use the '--no_restart' flag, which will exit the watchdog after application finish.

### Configuration

All the functions are configured via the xml-configfile, which elements will be explained in detail below.


The smallest possible configuration is this:

    <WatchdogConfig logfile="watch.log" watchFrequency="15">
       <Application binary="calc.exe"/>
    </WatchdogConfig>

It will start the application 'calc.exe' check its status every 15 seconds and log all watchdog output to the logfile 'watch.log'.

As an alternative to the Application-node you can configure a SwitchableApplications-node like this:

    <SwitchableApplications directory="folder" initial="application_watchdog_file_name_without_extension"/>

'directory' is where other xml-files for each application you want to switch to are located. These xml-Files contain *only* the Application-node as specified below. You can switch between these applications by sending the switch-command via UDP like this:

    switch/application_watchdog_file_name_without_extension

where 'switch' is the command specified in the SwitchApplication-node. The application initially started by watchdog is given in the 'initial' attribute of the SwitchableApplications-node.



The full feature set is divided in three categories:
1. The application startup configuration, timed restart commands and runtime checks, like memory consumption and heartbeat detection.
2. Systemcommand pre and post application execution and timed computer restart or shutdown
3. Udpcontrol interface for status and controlling of computer and application

All optional nodes are obsolete, the functionality will be disabled.
The notation `${env_var}` will evaluate the environment variable and is used in heartbeat_file definition, application binary, arguments and working directory.

The feature set in detail:

### Application execution

Full featured application configuration node:

    <Application binary="calc.exe" directory="" logFile="calc.log" logFormatter="%Y-%M-%D-%h-%m-%s" windowtitle"Calculator" showWindow="maximized|minimized">
        <EnvironmentVariables>
            <!--<EnvironmentVariable name="key"><![CDATA[value]]></EnvironmentVariable>-->
        </EnvironmentVariables>
        <Arguments>
            <Argument>test.txt</Argument>
        </Arguments>
        <Heartbeat>
            <Heartbeat_File>${TEMP}/heartbeat.xml</Heartbeat_File>
            <Allow_Missing_Heartbeats>5</Allow_Missing_Heartbeats>
            <Heartbeat_Frequency>1</Heartbeat_Frequency>
            <FirstHeartBeatDelay>120</FirstHeartBeatDelay>
        </Heartbeat>
        <WaitDuringStartup>0</WaitDuringStartup>
        <WaitDuringRestart>10</WaitDuringRestart>
        <Memory_Threshold>100000</Memory_Threshold>
        <RestartDay>Monday</RestartDay>
        <RestartTime>12:02</RestartTime>
        <CheckMemoryTime>00:00</CheckMemoryTime>
        <CheckTimedMemoryThreshold>150000</CheckTimedMemoryThreshold>
    </Application>

#### The \<Application> root-node defines the app to execute and watch

- Attributes are as follows:
  - 'binary'     - defines the binary filename
  - 'directory'  - watchdog changes to this directory before executing app [optional]
  - 'logFile'    - watchdog will redirect all stdout and stderr outputs to this filename [optional, Linux/OSX only]
  - 'logFormatter' - add a timestring in this format to the filename, empty string means use default [optional, Linux/OSX only]
  - 'showWindow' - maximized|minimized can be set, default is maximized [optional, Windows only]
  - 'windowtitle' - when set the watchdog tries to find the window with the title and if found gracefully stops or restarts the application. If not set the watchdog by default terminates the application [optional, Windows only]

- Optional nodes:

  - \<Arguments> defines a list of application arguments
    and has children of \<Argument>-nodes with childnode definition of the environment variable
    with the use of CDATA-definition it is possible to handle specials character easier (i.e. '\').

  - \<EnvironmentVariables> defines a list of environment variables, that will be set before app executing
    and has children of \<EnvironmentVariable>-nodes with key-value definition of the environment variable
    with the use of CDATA-definition it is possible to handle specials character easier (i.e. '\').

  - \<Heartbeat> Heartbeat detection will check the content of a given file and expects the seconds since 1970 in a format like this:
  \<heartbeat secondsSince1970="1332154365"/>
     - \<Heartbeat_File> will define the heartbeat file in its childnode.
     - \<Allow_Missing_Heartbeats> will define the allowed mssing heartbeat before the watchdog assumes a app to be dead.
     - \<Heartbeat_Frequency> will define expected frequency ot the heartbeat in seconds.
     - \<FirstHeartBeatDelay> will define delay in seconds, before the heartbeat detection begins (i.e. for a longer app starttime).

  - \<WaitDuringStartup> will define a startup time before the app starts
  - \<WaitDuringRestart> will define a wait time before the app restarts
  - \<RestartTime> will define a time string, at which the apps restarts
  - \<RestartDay> will define a day on which the \<RestartTime> will be used to restart the app
  - \<Memory_Threshold> will define a threshold of free system memory, the undercut of the threshold will lead to a  app restart
  - \<CheckMemoryTime> will define a threshold of free system memory which will be used with node \<CheckTimedMemoryThreshold> to check memory
     assumption at a given timestamp.
  - \<CheckTimedMemoryThreshold> will define a timestamp at which memory-assumption threshold from node \<CheckMemoryTime >will be used to check


### Systemcommand execution

Full featured systemcommand configuration node:

    <RebootTime>15:30</RebootTime>
    <HaltTime>16:10</HaltTime>
    <AppPreTerminateCommand>
        <![CDATA[dir]]>
    </AppPreTerminateCommand>
    <AppTerminateCommand ignoreOnUdpRestart="false">
        <![CDATA[dir]]>
    </AppTerminateCommand>
    <PreShutdownCommand>
        <![CDATA[dir]]>
    </PreShutdownCommand>
    <PreStartupCommand>
        <![CDATA[dir]]>
    </PreStartupCommand>
    <PreAppLaunchCommand>
    <![CDATA[dir]]>
    </PreAppLaunchCommand>
    <PostAppLaunchCommand>
    <![CDATA[dir]]>
    </PostAppLaunchCommand>

Fully Optional nodes:

- \<RebootTime> will define a timestamp at which the computer will reboot
- \<HaltTime> will define a timestamp at which the computer will shutdown
- \<AppPreTerminateCommand> will define a systemcommand to be executed before the app will be terminated
- \<AppTerminateCommand> will define a systemcommand to be executed after the app terminated
   - attributes:
        - `ignoreOnUdpRestart` - can be made dependent if the app is restarted via udp or exited internally
- \<PreShutdownCommand> will define a systemcommand to be executed before the computer is shutdown
- \<PreStartupCommand> will define a systemcommand to be executed when the watchdog is started
- \<PreAppLaunchCommand> will define a systemcommand to be executed before the application is launched
- \<PostAppLaunchCommand> will define a systemcommand to be executed when the application has launched

### Udpcontrol interface

Full featured Udpcontrol configuration node:

    <UdpControl port="2346" returnmessage="false" returnMessagePort="-1">
        <IpWhitlelist>
            <Ip>10.1.3.91</Ip>
            <Ip>127.0.0.1</Ip>
        </IpWhitlelist>
        <SystemHalt command="halt"/>
        <SystemReboot command="reboot"/>
        <RestartApplication command="restart"/>
        <SwitchApplication command="switch_app"/>
        <StopApplication command="stop"/>
        <StartApplication command="start"/>
        <StatusReport command="status" loadingtime="2"/>
        <ContinuousStatusChangeReport ip="10.1.1.106" port="6655"/>
    </UdpControl>

#### The \<UdpControl> root-node enabled the udp control functionality of the watchdog

- Attributes:
  - 'port'    - port the watchdog will listen to
  - 'returnmessage' - flag to toggle if the watchdog will return all messages to the client using the sender port. Default is 'false'
  - 'returnMessagePort' - port the watchdog will return messages to, default (-1 or unset) is incoming port

- Optional:
  - \<IpWhitlelist> defines a list of ip-adresses, for which the watchdog allows udp control
    and has children of \<Ip>-nodes that define the whitelist. If defined only senderhost with the
    configured ip will be accepted.

  - \<SystemHalt> will add the listener to the specified command, if a udp-packet with this content is accepted, the computer
    will shutdown

  - \<SystemReboot> will add the listener to the specified command, if a udp-packet with this content is accepted, the computer will reboot

  - \<RestartApplication> will add the listener to the specified command, if a udp-packet with this content is accepted, the watchdog will restart the application

  - \<SwitchApplication> will add the listener to the specified command, if a udp-packet with this content is accepted, the watchdog will restart the application with the watchdog-file identified by 'id'.

  - \<StopApplication> will add the listener to the specified command, if a udp-packet with this content is accepted, the watchdog will stop the application

  - \<StartApplication> will add the listener to the specified command, if a udp-packet with this content is accepted, the watchdog  will start the application

  - \<StatusReport> will add the listener to the specified command, if a udp-packet with this content is accepted, the watchdog sends back the status of the application [running, loading, terminated]. If the attribute \<loadingtime> in seconds is specified, the status  loading will be send for the given amount of seconds to simulate loading procedure

  - \<ContinuousStatusChangeReport> triggers a continuous status change udp command to given ip and port

# Good Practices

 - If you need to ensure that your application is properly terminated instead of just shut down when the watch dog receives a system halt you can use the preshutdown command with taskkill on windows
    `<PreShutdownCommand>taskkill /IM process1.exe /IM process2.exe</PreShutdownCommand>`
 - You can test udp commands and related watchdog setup with netcat. Open a connection with `nc -u ip port` and enter the udp commands like halt as specified in the watchdog file 
