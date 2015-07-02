MODULE_NAME='duet-lib-pjlink_dr0_1_2' (DEV dvDuetDevice)
//MODULE_NAME='duet-lib-pjlink_dr1_0_0' (DEV dvDuetDevice, DEV dvPhysicalDevice)
(*{{PS_SOURCE_INFO(PROGRAM STATS)                          *)
(***********************************************************)
(*  ORPHAN_FILE_PLATFORM: 1                                *)
(***********************************************************)
(*}}PS_SOURCE_INFO                                         *)
(***********************************************************)

(***********************************************************)
(*               VARIABLE DEFINITIONS GO BELOW             *)
(***********************************************************)
DEFINE_VARIABLE

// Setup Duet Module properties
CHAR DUET_PROPERTIES[9][47] = 
{
	'Physical-Device',
	'Duet-Device',
	'Duet-Module=duet-lib-pjlink_dr0_1_2',
	'Bundle-Version=0.1.2',
	'Device-Category=ip',
	'Device-Make=Alex McLain',
	'Device-Model=PJLink',
	'Device-SDKClass=com.amx.duet.devicesdk.Utility',
	'Device-Revision=0.1.2'
}


(***********************************************************)
(*                STARTUP CODE GOES BELOW                  *)
(***********************************************************)
DEFINE_START

// Load up device numbers as strings
//DUET_PROPERTIES[1] = "'Physical-Device=',FORMAT('%d:',dvPhysicalDevice.NUMBER),FORMAT('%d:',dvPhysicalDevice.PORT),FORMAT('%d',dvPhysicalDevice.SYSTEM)";
DUET_PROPERTIES[1] = "'Physical-Device=0:0:0'";
DUET_PROPERTIES[2] = "'Duet-Device=',FORMAT('%d:',dvDuetDevice.NUMBER),FORMAT('%d:',dvDuetDevice.PORT),FORMAT('%d',dvDuetDevice.SYSTEM)";

// Load Duet Module
LOAD_DUET_MODULE(DUET_PROPERTIES)

(***********************************************************)
(*                     END OF PROGRAM                      *)
(*        DO NOT PUT ANY CODE BELOW THIS COMMENT           *)
(***********************************************************)
