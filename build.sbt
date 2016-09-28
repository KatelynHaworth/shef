name := "shef"

organization := "au.id.haworth"

version := "0.1.0"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "commons-codec"                        % "commons-codec"             % "1.10",
  "org.bouncycastle"                     % "bcprov-jdk15on"           % "1.53",
  "org.bouncycastle"                     % "bcpkix-jdk15on"           % "1.53",
  "io.spray"                            %% "spray-client"             % "1.3.2",
  "io.spray"                            %% "spray-json"               % "1.3.2",
  "io.spray"                            %% "spray-can"                % "1.3.2",
  "net.virtual-void"                    %% "json-lenses"              % "0.6.1",
  "com.typesafe.akka"                   %% "akka-actor"               % "2.3.6"
)
