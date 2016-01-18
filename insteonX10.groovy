/**
 *  Insteon X10 Switch/Dimmer (LOCAL)
 *
 *  Copyright 2015 umesh31@gmail.com
 *  Copyright 2014 patrick@patrickstuart.com
 *  Updated 1/4/15 by goldmichael@gmail.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
def static ON = "280"
def static OFF = "380"
def static DIM = "480"
def static BRIGHT = "580"
def static INTER_COMMAND_SLEEP_MS = 600
metadata {
  definition (name: "Insteon X10 Switch (LOCAL)", namespace: "pshotton", author: "umesh31@gmail.com/patrick@patrickstuart.com/tslagle13@gmail.com/goldmichael@gmail.com/phil.shotton@cloudscapesolutions.com") {
    capability "Switch Level"
    capability "Actuator"
    capability "Switch"
  }
  
  preferences {
    input("InsteonIP", "string", title:"Insteon IP Address", description: "Please enter your Insteon Hub IP Address", defaultValue: "192.168.1.2", required: true, displayDuringSetup: true)
    input("InsteonPort", "string", title:"Insteon Port", description: "Please enter your Insteon Hub Port", defaultValue: "25105", required: true, displayDuringSetup: true)
    input("InsteonHubUsername", "string", title:"Insteon Hub Username", description: "Please enter your Insteon Hub Username", defaultValue: "user" , required: true, displayDuringSetup: true)
    input("InsteonHubPassword", "password", title:"Insteon Hub Password", description: "Please enter your Insteon Hub Password", defaultValue: "password" , required: true, displayDuringSetup: true)
    input("X10HouseCode", "string", title:"Device Housecode", description: "Please enter the device HouseCode", defaultValue: "A", required: true, displayDuringSetup: true)
    input("X10UnitCode", "string", title:"Device Unitcode", description: "Please enter the device UnitCode", defaultValue: "1", required: true, displayDuringSetup: true)
  }
  
  simulator {
    // status messages
    status "on": "on/off: 1"
    status "off": "on/off: 0"
    
    // reply messages
    reply "zcl on-off on": "on/off: 1"
    reply "zcl on-off off": "on/off: 0"
  }
  
  // UI tile definitions
  tiles {
    standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
      state "on", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#79b821", nextState:"turningOff"
      state "off", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"turningOn"
      state "turningOn", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#79b821", nextState:"turningOff"
      state "turningOff", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"turningOn"
    }
    controlTile("levelSliderControl", "device.level", "slider", height: 1, width: 3, inactiveLabel: false) {
      state "level", action:"switch level.setLevel"
    }
    
    main(["switch"])
    details(["switch", "levelSliderControl"])
  }
}

// handle commands

def on() {
  //log.debug "Executing 'take'"
    sendEvent(name: "switch", value: "on")
    sendEvent(name: "level", value: 100)
    sendCmd(ON)
}

def off() {
  log.debug("off")
    sendEvent(name: "switch", value: "off")
    sendEvent(name: "level", value: 0)
    sendCmd(OFF)
}


private String convertIPtoHex(ipAddress) { 
  String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
  return hex

}

private String convertPortToHex(port) {
  String hexport = port.toString().format( '%04x', port.toInteger() )
  return hexport
}


def createHubAction(path){
  log.debug "path is: $path"
  
  def hosthex = convertIPtoHex(InsteonIP)
  def porthex = convertPortToHex(InsteonPort)
  /*device.deviceNetworkId = "$hosthex:$porthex:${InsteonID}" 
  log.debug device.deviceNetworkId
  */
  def userpassascii = "${InsteonHubUsername}:${InsteonHubPassword}"
  def userpass = "Basic " + userpassascii.encodeAsBase64().toString()
  def headers = [:] //"HOST:" 
  headers.put("HOST", "$InsteonIP:$InsteonPort")
  headers.put("Authorization", userpass)
  
  try {
    def hubAction = new physicalgraph.device.HubAction(
      method:  "GET",
      path: path,
      headers: headers
    )
    log.debug hubAction
    hubAction
  }
  catch (Exception e) {
    log.debug "Hit Exception on $hubAction"
    log.debug e
  }
}

def sendCmd(cmd){
    def addressCommand = getX10AddressMessage()
    sendHubCommand(createHubAction(addressCommand))
    Thread.sleep(INTER_COMMAND_SLEEP_MS) // seems to be necessary!
    addressCommand = "/3?0263${X10HouseCode}${cmd}=I=3"
    return createHubAction(addressCommand)
}

def getX10AddressMessage() {
    "/3?0263${X10HouseCode}${X10UnitCode}=I=3"
}

/**
 * Insteon X11 has 22 levels of brightness, bright and dim commands raise/lower by 1 level
 * So calculate number of up/down commands to send from diff in old vs new level
 * @param value
 * @return
 */
def setLevel(value) {
  log.debug "setting level ${value}"

  def level = 255*(Math.min(value as Integer, 99))/100
  level = Integer.toHexString(level as Integer )

  log.debug "setting level ${level}"

  if(value == 0){
    return off()
  }
  sendEvent(name: "switch", value: "on")

//  if(level.size() == 1){
//    sendCmd("0${level}")
//  }else{
//    sendCmd("${level}")
//  }
}

def setLevel(value, duration) {
  log.debug "setting level ${value} ${duration}"
}

def parse(String description) {
  log.debug("parse called with: ${description}")
}
