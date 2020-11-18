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

    var resultArray = [];

    var params = {
        queryString: 'shadow.reported.location:* AND thingTypeName:LFD'
    };

    try {

        var result = await iotClient.searchIndex(params).promise();

        //build an array of the thing shadow values and return array
        result.things.forEach(element => {

            var shadow = JSON.parse(element.shadow);

            shadow.reported.lfdId = element.thingName;

            resultArray.push(shadow.reported);
        });

        return resultArray;
    }
    catch (err) {

        console.log("error: " + err);
        throw err;
    }
};