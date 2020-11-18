/* Amplify Params - DO NOT EDIT
	ENV
	REGION
Amplify Params - DO NOT EDIT */

const AWS = require('aws-sdk');
const iotClient = new AWS.Iot();

var region = process.env.REGION;

AWS.config.update({
    region: region
});

exports.handler = async (event) => {

    //query for the lfd confirming it has a reported shadow
    //you must have fleet indexing enabled in IoT Core with REGISTRY_AND_SHADOW indexed

    const lfdId = event.arguments.lfdId || "";

    try {
        var params = {
            queryString: 'shadow.reported.location:* AND thingName:' + lfdId
        };

        var result = await iotClient.searchIndex(params).promise();

        if (result.things.length > 0) {

            var element = result.things[0];

            var shadow = JSON.parse(element.shadow);

            shadow.reported.lfdId = element.thingName;

            return shadow.reported;

        } else {

            throw new Error("LFD not found:" + lfdId);
        }
    }
    catch (err) {
        console.log("error: " + err);
        throw new Error("Error retrieving LFD: " + lfdId);
    }
};
