/**
 *  Insteon X10 Dimmer (LOCAL)
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


metadata {
    definition (name: "Insteon X10 Dimmer (LOCAL)", namespace: "pshotton", author: "umesh31@gmail.com/patrick@patrickstuart.com/tslagle13@gmail.com/goldmichael@gmail.com/phil.shotton@cloudscapesolutions.com") {
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
    sendEvent(name: "switch", value: "on")
    def level = state.level
    if (level == null) {
        level = 100
        state.level = level
        sendEvent(name: "level", value: level)
    }
    def addressCommand = getX10AddressMessage()
    def commandCommand = getX10ControlMessage("280")
    return [ createHubAction(addressCommand), createHubAction(commandCommand) ]
}

def off() {
    log.debug("off")
    sendEvent(name: "switch", value: "off")
    def addressCommand = getX10AddressMessage()
    def commandCommand = getX10ControlMessage("380")
    return [ createHubAction(addressCommand), createHubAction(commandCommand) ]
}

/**
 * Insteon X11 has 22 levels of brightness, bright and dim commands raise/lower by 1 level
 * So calculate number of up/down commands to send from diff in old vs new level
 * @param value
 * @return
 */
def setLevel(value) {
    log.debug "setting level ${value}"
    def current = X10Level(state.level)
    if (current == null) {
        current = 0
        state.level = 0
    }
    def diff = current - X10Level(value)
    def cmd = "480" // dim
    if (diff < 0) { // is current less than desired, if so brighten
        cmd = "580"
        diff = -diff
    }

    def hubCommands = [createHubAction(getX10AddressMessage())]

    for (int i = 0; i < diff; i++) {
        hubCommands.add(createHubAction(getX10ControlMessage(cmd)))
    }
    state.level = value
    sendEvent(name: "switchLevel", value: value)
    hubCommands
}

def setLevel(value, duration) {
    log.debug "setting level ${value} ${duration}"
}

def parse(String description) {
    log.debug("parse called with: ${description}")
}


def createHubAction(path){
    def userpassascii = "${InsteonHubUsername}:${InsteonHubPassword}"
    def userpass = "Basic " + userpassascii.encodeAsBase64().toString()
    def headers = [:]
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

def getX10AddressMessage() {
    "/3?0263${getHouseCode()}${getUnitCode()}=I=3"
}

def getX10ControlMessage(cmd) {
    "/3?0263${getHouseCode()}${cmd}=I=3"
}

def getHouseCode() {
    def codes = [
            'A':'6',
            'B':'E',
            'C':'2',
            'D':'A',
            'E':'1',
            'F':'9',
            'G':'5',
            'H':'D',
            'I':'7',
            'J':'F',
            'K':'3',
            'L':'B',
            'M':'0',
            'N':'8',
            'O':'4',
            'P':'C']
    return codes[X10HouseCode]
}

def getUnitCode() {
    def codes = [
            '1':'600',
            '2':'E00',
            '3':'200',
            '4':'A00',
            '5':'100',
            '6':'900',
            '7':'500',
            '8':'D00',
            '9':'700',
            '10':'F00',
            '11':'300',
            '12':'B00',
            '13':'000',
            '14':'800',
            '15':'400',
            '16':'C00'
    ]
    return codes[X10UnitCode]
}

def X10Level(level) {
    if (level == null) {
        return null
    }
    def x10level = 22 * (int)level / 100
    log.debug "Level ${level} maps to ${(int)x10level}"
    (int)x10level
}
