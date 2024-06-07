#include <SoftwareSerial.h>

SoftwareSerial BTSerial(3,4);

void setup() {
  Serial.begin(9600);  
  BTSerial.begin(9600);
}

void loop() {
  BTSerial.println('1');
  delay(100);
}
