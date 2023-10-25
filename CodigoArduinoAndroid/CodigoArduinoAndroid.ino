#define potenciometer A1
#include <ArduinoJson.h>

void setup() {
  Serial.begin(9600);
}

void loop() {
  StaticJsonDocument<200> jsonDocument;

  int potenciometerValue = analogRead(potenciometer);

  jsonDocument["sensor"] = "potenciometer";
  jsonDocument["value"] = potenciometerValue;

  String jsonString;
  serializeJson(jsonDocument, jsonString);

  Serial.println(jsonString);

  delay(100);
}
