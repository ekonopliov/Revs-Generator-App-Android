#include <OBD2UART.h>
#include <SoftwareSerial.h>

SoftwareSerial mySerial(11, 10);
COBD obd;
int value;
int oldValue = 0;
byte bytes[20];
bool zoneEntered = false;

void setup()
{
  mySerial.begin(9600);
  obd.begin();
  while (!obd.init());
}

void loop()
{
 if (obd.readPID(PID_RPM, value)) {
  if(value > 2800){
    zoneEntered = true;
  }
  if(zoneEntered){
  if(oldValue - value > 300) {     //revs
    delay(10);
    mySerial.write(53);
    mySerial.write(10);
    zoneEntered = false;
    delay(2000);
    }
    oldValue = value;
  }
  
    String stringValue = String(value);
    stringValue.getBytes(bytes,20);
    for(int i = 0; i < stringValue.length(); i++){
    mySerial.write(bytes[i]);
    }
    mySerial.write(10);
 }
}
