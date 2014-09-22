# AMX DUET PJLINK MODULE

duet-lib-pjlink

This library contains the code for NetLinx or Duet projects to interface with video projectors that implement the PJLink protocol over TCP/IP. This class adheres to the Java 1.4ME API to maintain compatibility with AMX Duet devices.

## Overview

[TOC]


## Download

**Git Users:**

https://github.com/amclain/duet-lib-pjlink


**Mercurial Users:**

https://bitbucket.org/amclain/duet-lib-pjlink


**Zip File:**

Both sites above offer a feature to download the source code as a zip file.
Any stable release, as well as the current development snapshot can be downloaded.

## Issues, Bugs, Feature Requests

Any bugs and feature requests should be reported on the GitHub issue tracker:

https://github.com/amclain/duet-lib-pjlink/issues


**Pull requests are preferred via GitHub.**

Mercurial users can use [Hg-Git](http://hg-git.github.io/) to interact with
GitHub repositories.

## Known Issues

* The NetLinx `pulse` keyword can trigger the on/off channel events in a way that
doesn't get passed to the Duet module. Therefore `on` and `off` should be used.
Wrapping the call to `off` in a 1 second wait time seems to be a valid workaround.

* Error status channels may not work correctly.

## Examples

### Channel Numbers

The full list of channel numbers can be found in the file [PJLinkModule.java](https://github.com/amclain/duet-lib-pjlink/blob/master/com/alexmclain/duet/pjlink/dr0_1_1/PJLinkModule.java)

### Setting Up The Module
``` c
(***********************************************************)
(*           DEVICE NUMBER DEFINITIONS GO BELOW            *)
(***********************************************************)
DEFINE_DEVICE

dvPROJ            = 41001:1:0;

(***********************************************************)
(*              CONSTANT DEFINITIONS GO BELOW              *)
(***********************************************************)
DEFINE_CONSTANT

// Duet Device Channels
PROJ_POWER_ON           = 27;
PROJ_POWER_OFF          = 28;

PROJ_PICTURE_MUTE       = 211;

PROJ_WARMING            = 253;
PROJ_COOLING            = 254;
PROJ_LAMP_ON            = 255;

// Projector Sources
PROJ_SOURCE_VGA_1       = 311;
PROJ_SOURCE_HDMI_1      = 332;

(***********************************************************)
(*                 STARTUP CODE GOES BELOW                 *)
(***********************************************************)
DEFINE_START

define_module 'duet-lib-pjlink_dr0_1_1' pj1(dvPROJ);

(***********************************************************)
(*                   THE EVENTS GO BELOW                   *)
(***********************************************************)
DEFINE_EVENT

data_event[dvPROJ]
{
    online:
    {
        set_virtual_channel_count(data.device, 512);
        
        // Set projector IP address.
        send_command data.device, 'IPADDR=192.168.1.12';
    }
    
    command:
    {
        if (find_string(data.text, 'LAMPTIME-', 1) > 0)
        {
            lampHours = atoi(right_string(data.text, length_string(data.text) - 9));
        }
    }
}

// Switch to input after warmup.
channel_event[dvPROJ, PROJ_LAMP_ON]
{
    on:
    {
        wait 600
        {
            cancel_wait 'input_select_proj'
            on[dvPROJ, PROJ_SOURCE_HDMI_1];
            wait 10 'input_select_proj' off[dvPROJ, PROJ_SOURCE_HDMI_1];
        }
    }
    
    off: {}
}
```

### Triggering Projector Actions
``` c
// Projector Controls
button_event[vdvTP, BTN_PROJECTOR_ON]
{
    push:
    {
        on[dvPROJ, PROJ_POWER_ON];
        wait 10 { off[dvPROJ, PROJ_POWER_ON]; }
    }
    
    release: {}
}

button_event[vdvTP, BTN_PROJECTOR_OFF]
{
    push:
    {
        on[dvPROJ, PROJ_POWER_OFF];
        wait 10 { off[dvPROJ, PROJ_POWER_OFF]; }
    }
    
    release: {}
}
```
