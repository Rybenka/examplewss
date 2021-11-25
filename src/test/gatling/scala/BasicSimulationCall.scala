import io.gatling.core.Predef.{exec, _}
import io.gatling.http.Predef._

import java.nio.file.{Files, Paths}
import scala.concurrent.duration.DurationInt

class BasicSimulationCall extends Simulation {

  val byteArray = Files.readAllBytes(Paths.get("src/test/gatling/resources/dummyBinaryFile"))
  val httpProtocol = http
    .baseUrl("http://localhost:8080/v1/")
    .wsBaseUrl("ws://localhost:8080/v1/")
    .wsReconnect
    .wsMaxReconnects(5)
    .disableWarmUp
  val chunkSize: Int = 8000


  def sendToWS = {
      exec(
        ws("WS Connect")
          .connect("123/521beea1-2cd1-4fa3-b087-e862a39e1000/1")
          .await(300.millis)(
            ws.checkTextMessage("checkConnection")
              .check(jsonPath("$.message").is("Connection - OK"))
          )
          .onConnected(
            exec(
              ws("Send Config Frame")
                .sendText(s"{'ChannelId': 'ch-123', 'OperatorId': 'JohnDoe'}")
                .await(500.second)(
                  ws.checkTextMessage("checkCfgFrame")
                    .check(jsonPath("$.message").is("Config Frame - OK"))
                )
            ).pause(500.millis)
          )
      )
      .pause(1)
      .foreach(byteArray.grouped(chunkSize).toSeq, "packet") {
        exec(
          ws("Send bytes").sendBytes("${packet}")
          //Here we don't wait for any response message
        ).pause(250.millis)
      }
      .pause(100.millis)
      .exec(ws("Close WS").close)
  }

    val scn = scenario("Web Socket test")
      .exec(sendToWS)

  setUp(
        scn.inject(rampUsers(1).during(1)).protocols(httpProtocol)
  )
}