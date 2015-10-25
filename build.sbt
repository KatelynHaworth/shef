name := "shef"

organization := "au.id.haworth"

version := "0.1.0"

scalaVersion := "2.10.6"

libraryDependencies ++= Seq(
  "commons-codec"                       % "commons-codec"             % "1.10",
  "org.apache.httpcomponents"           % "httpclient"                % "4.5.1",
  "org.bouncycastle"                    % "bcprov-jdk15on"            % "1.53",
  "org.bouncycastle"                    % "bcpkix-jdk15on"            % "1.53"
)
