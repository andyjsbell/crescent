/* Amplify Params - DO NOT EDIT
	API_CRESCENT_GRAPHQLAPIENDPOINTOUTPUT
	API_CRESCENT_GRAPHQLAPIIDOUTPUT
	ENV
	REGION
Amplify Params - DO NOT EDIT */

const https = require('https');
const AWS = require('aws-sdk');
const urlParse = require("url").URL;
const createState = /* GraphQL */ `
  mutation CreateState(
    $input: CreateStateInput!
    $condition: ModelStateConditionInput
  ) {
    createState(input: $input, condition: $condition) {
      id
      lfdId
      upTime
      OPS
      Front
      HDMI1
      HDMI2
      HDMI3
      DP
      VGA
      usb
      brightness
      createdAt
      updatedAt
    }
  }
`;

//environment variables
const region = process.env.REGION
const appsyncUrl = process.env.API_CRESCENT_GRAPHQLAPIENDPOINTOUTPUT
const endpoint = new urlParse(appsyncUrl).hostname.toString();

exports.handler = async (event) => {
    console.log('event received:' + JSON.stringify(event));

    const req = new AWS.HttpRequest(appsyncUrl, region);
    const item = {
        input: {
            lfdId: event.lfdId,
            upTime: event.upTime,
            OPS: event.OPS,
            Front: event.Front,
            HDMI1: event.HDMI1,
            HDMI2: event.HDMI2,
            HDMI3: event.HDMI3,
            DP: event.DP,
            VGA: event.VGA,
            usb: event.usb,
            brightness: event.brightness
        }
    };

    //execute the mutation
    try {
        req.method = "POST";
        req.headers.host = endpoint;
        req.headers["Content-Type"] = "application/json";
        req.body = JSON.stringify({
            query: createState,
            operationName: "CreateState",
            variables: item
        });

        const signer = new AWS.Signers.V4(req, "appsync", true);
        signer.addAuthorization(AWS.config.credentials, AWS.util.date.getDate());

        const data = await new Promise((resolve, reject) => {
            const httpRequest = https.request({ ...req, host: endpoint }, (result) => {
                result.on('data', (data) => {
                    resolve(JSON.parse(data.toString()));
                });
            });

            httpRequest.write(req.body);
            httpRequest.end();

        });

        console.log("Successful mutation");

        return {
            statusCode: 200,
            body: data
        };

    }
    catch (err) {
        console.log("error: " + err);
        throw new Error("Error creating lfd value for lfd: " + event.sensorId);
    }
};
