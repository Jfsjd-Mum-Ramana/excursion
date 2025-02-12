1. Get Scatter Plot Data API

Purpose:
Retrieves both collection and metric data based on selected protocol and data type (e.g., number of messages or size of messages) to populate the scatter plot.

Endpoint:

GET /api/alarms/scatter-plot-data


Request Parameters:
 `protocol`: (SNMP, WSS, etc.)
 `data_type`: (e.g., "number_of_msgs", "size_of_msgs")

Response:


{
  "scatter_plot_data": [
    {
      "collection_name": "Collection1" // Represents protocal names
      "date": "2024-11-20T10:30:00Z",  // Represents last updated
      "y_value": 204800 // Represents size of messages OR number of messages
    }
  ]
}


Why We Need It:

- This API is used to fetch the data required for dynamically rendering the scatter plot. It ensures that only relevant data (based on the user's protocol and data type selection) is fetched.



2. Filter Data by Protocol API

Purpose:
Filters the available collections by the selected protocol (e.g., SNMP, WSS, gRPC), allowing users to visualize only the data for a specific protocol.

Endpoint:

GET /api/alarms/scatter-plot-data/{protocal}


Request Parameters:
- `protocol`: (SNMP, WSS, gRPC, etc.)

Response:


{
  "scatter_plot_data": [
    {
      "collection_name": "Collection1" // Represents protocal names
      "date": "2024-11-20T10:30:00Z",  // Represents last updated
      "y_value": 204800 // Represents size of messages OR number of messages
    }
  ]
}


Why We Need It:

- This ensures that the scatter plot can be filtered based on the selected protocol, allowing users to narrow down the data they are visualizing.



3. Get Available Protocols API

Purpose:
Fetches the list of protocols supported, so users can select which protocol they want to visualize data for.

Endpoint:

GET /api/alarms/protocols


Response:


{
  "protocols": ["SNMP", "WSS", "gRPC", "TL1", "syslog"]
}


Why We Need It:

- The frontend needs to present available protocols for selection, allowing the user to choose which protocol’s data to visualize in the scatter plot.
