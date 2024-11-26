import { Box, Button, Card, CardContent, Container, FormControl, Grid, InputLabel, MenuItem, Select, SelectChangeEvent, TextField, Typography } from '@mui/material';
import { DataGrid, GridColDef, GridToolbar } from '@mui/x-data-grid';
import axios from 'axios';
import React, { useEffect, useState } from 'react';
import { authenticate, getToken } from '../auth';
import { __UCS_GET_DEVICE_DEATIL_BY_TYPE__, __UCS_GRAPHQL_URL__ } from '../api-endpoints';
import { UNIQUE_MODELS_QUERY, UNIQUE_VENDORS_QUERY, UNIQUE_NETWORKS_QUERY, GET_DEVICE_DETAILS } from '../graphQL/graphqlQueries';

interface Device {
    id: number;
    deviceName: string;
    model: string;
    loopback: string;
    status: string;
    vendor: string;
    router_type: string;
    poller_cluster: string;
    poll_interval: string;
    network: string;
    last_update: string;
    phys_ip_address: string;
}
interface ActualApiResponse {
    status: number,
    message: string,
    data: DeviceResponse[];
    error: string | null;
}
interface DeviceResponse {
    deviceName: string,
    name: string,
    asn: string,
    subAsn: string,
    model: string,
    loopback: string,
    status: string,
    hubName: string,
    vendor: string,
    routerType: string,
    usage: string,
    snmpCommString: string,
    codeVersion: string,
    subtechnology: string,
    pollerCluster: string,
    pollInterval: string,
    network: string,
    maxOids: string,
    maxPduSize: string,
    maxRetries: string,
    maxPduPerSec: string,
    wugThreads: string,
    uptime: string,
    indxType: string,
    pollerInterval: string,
    lastUpdate: string,
    pollerclusterlov: string,
    pollerclusteralarm: string,
    physIpAddress: string,
    locationCode: string,
    physIp: string,
    functionalType: string,
    shelfType: string,
    telemMngdNetwork: string,
    domainFlag: string
}
interface ApiRequest {
    deviceName: string;
    model: string;
    loopback: string;
    vendor: string;
    network: string;
}



const DashboardGridData = () => {

    const [selectedDeviceName, setSelectedDeviceName] = useState<string>('');
    const [selectedDeviceIp, setSelectedDeviceIp] = useState<string>('');
    const [selectedDeviceModel, setSelectedDeviceModel] = useState<string>('');
    const [selectedDeviceVendor, setSelectedDeviceVendor] = useState<string>('');
    const [selectedDeviceNetwork, setSelectedDeviceNetwork] = useState<string>('');
    const [data, setData] = useState<Device[]>([]);
    const [loading, setLoading] = useState<boolean>(false);
    // const [deviceNames, setDeviceNames] = useState<string[]>([]);
    // const [deviceIps, setDeviceIps] = useState<string[]>([]);
    const [deviceModels, setDeviceModels] = useState<string[]>([]);
    const [deviceVendors, setDeviceVendors] = useState<string[]>([]);
    const [deviceNetworks, setDeviceNetworks] = useState<string[]>([]);


    useEffect(() => {
        const fetchDropDownData = async () => {
            try {
                //     const pw = __UCS_API_PW__;
                //    await authenticate("Eclipse", __UCS_API_PW__);
                //     const token = getToken();
                //     const headers = { Authorization: `Bearer ${token}` }
                const responses = await Promise.all([
                    await axios.post(__UCS_GRAPHQL_URL__, {
                        query: UNIQUE_MODELS_QUERY,
                    }),
                    await axios.post(__UCS_GRAPHQL_URL__, {
                        query: UNIQUE_VENDORS_QUERY,
                    }),
                    await axios.post(__UCS_GRAPHQL_URL__, {
                        query: UNIQUE_NETWORKS_QUERY,
                    }),

                ]);

                setDeviceModels(responses[0].data.data.uniqueModels);
                setDeviceVendors(responses[1].data.data.uniqueVendors);
                setDeviceNetworks(responses[2].data.data.uniqueNetworks);
            } catch (error) {
                console.error('Error fetching dropdown data', error);
            }
        };

        fetchDropDownData();

    }, []);

    const handleSelectChange = (setter: React.Dispatch<React.SetStateAction<string>>, resetOthers: () => void) => (
        event: SelectChangeEvent<string>) => {
        resetOthers();
        setter(event.target.value as string);
    };

    const handleTextFieldChange = (setter: React.Dispatch<React.SetStateAction<string>>, resetOthers: () => void) => (
        event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
        resetOthers();
        setter(event.target.value);
    };

    const resetSelections = () => {
        setSelectedDeviceName('');
        setSelectedDeviceIp('');
        setSelectedDeviceModel('');
        setSelectedDeviceVendor('');
        setSelectedDeviceNetwork('');
    }

    const handleSubmit = async () => {
        setLoading(true);
        const apiRequest: ApiRequest = {
            deviceName: selectedDeviceName,
            model: selectedDeviceModel,
            loopback: selectedDeviceIp,
            vendor: selectedDeviceVendor,
            network: selectedDeviceNetwork,
        };
        try {
            const token = getToken();
            const headers = { Authorization: `Bearer ${token}` }
            if (selectedDeviceName || selectedDeviceIp || selectedDeviceModel || selectedDeviceVendor || selectedDeviceNetwork) {
                //const response = await axios.get<ActualApiResponse>(__UCS_GET_DEVICE_DEATIL_BY_TYPE__, {params:apiRequest});
                const query = GET_DEVICE_DETAILS(selectedDeviceName, selectedDeviceModel, selectedDeviceIp, selectedDeviceVendor, selectedDeviceNetwork);
                const response = await axios.post(__UCS_GRAPHQL_URL__, {
                    query
                });
                const selectedDevices = response.data.data.searchDevices.map((device: DeviceResponse, index: number) => ({
                    id: index,
                    deviceName: device.deviceName,
                    model: device.model,
                    loopback: device.loopback,
                    status: device.status,
                    vendor: device.vendor,
                    router_type: device.routerType,
                    poller_cluster: device.pollerCluster,
                    poll_interval: device.pollerInterval,
                    network: device.network,
                    last_update: device.lastUpdate,
                    phys_ip_address: device.physIp,
                })
                );
                setData(selectedDevices);

            }
        } catch (error) {
            console.error('Error fetching data', error);
        } finally {
            setLoading(false);
        }
    };

    const columns: GridColDef[] = [
        { field: 'deviceName', headerName: 'DeviceName', minWidth: 150, flex: 1 },
        { field: 'model', headerName: 'DeviceModel', minWidth: 150, flex: 1 },
        { field: 'loopback', headerName: 'Loopback', minWidth: 150, flex: 1 },
        { field: 'status', headerName: 'Status', minWidth: 150, flex: 1 },
        { field: 'vendor', headerName: 'Vendor', minWidth: 150, flex: 1 },
        { field: 'router_type', headerName: 'RouterType', minWidth: 150, flex: 1 },
        { field: 'poller_cluster', headerName: 'PollerCluster', minWidth: 150, flex: 1 },
        { field: 'poll_interval', headerName: 'PollInterval', minWidth: 150, flex: 1 },
        { field: 'network', headerName: 'Network', minWidth: 150, flex: 1 },
        { field: 'last_update', headerName: 'LastUpdate', minWidth: 150, flex: 1 },
        { field: 'phys_ip_address', headerName: 'PhysIpAdd', minWidth: 150, flex: 1 },

    ];

    return (
        <Container maxWidth={false}>
            {/* <Typography gutterBottom  variant="titleXL" className="header" fontWeight={"bold"} gap={"100px"} paddingTop={"15px"}>
                UCS Portal
            </Typography> */}
            <Card sx={{ boxShadow: 1, border: 1, }}>
                <CardContent>
                    <Grid container spacing={2} alignItems={'flex-end'}>
                        <Grid item xs={12} sm={6} md>
                            <TextField
                                placeholder="Enter Device Name"
                                fullWidth
                                variant="outlined"
                                value={selectedDeviceName}
                                onChange={(event) => handleTextFieldChange(setSelectedDeviceName, resetSelections)(event)}
                                label="Device Name"
                            >
                            </TextField>
                        </Grid>
                        <Grid item xs={12} sm={6} md >
                            <TextField
                                placeholder="Enter Device IP"
                                fullWidth
                                variant="outlined"
                                value={selectedDeviceIp}
                                onChange={(event) => handleTextFieldChange(setSelectedDeviceIp, resetSelections)(event)}
                                label="Device IP"
                            >
                            </TextField>
                        </Grid>
                        <Grid item xs={12} sm={6} md>
                            <FormControl variant="outlined" sx={{ width: '100%', height: '100%' }}>
                                <InputLabel id="device-model-select-label" shrink={true}>Device Model</InputLabel>
                                <Select
                                    labelId="device-model-select-label"
                                    value={selectedDeviceModel}
                                    onChange={(event) => handleSelectChange(setSelectedDeviceModel, resetSelections)(event)}
                                    label="Device Model"
                                >
                                    {deviceModels?.map((model, i) => (
                                        <MenuItem
                                            key={model}
                                            id="deviceModel"
                                            value={model}
                                        >
                                            {model}
                                        </MenuItem>
                                    ))}
                                </Select>
                            </FormControl>
                        </Grid>
                        <Grid item xs={12} sm={6} md>
                            <FormControl variant="outlined" sx={{ width: '100%', height: '100%' }}>
                                <InputLabel id="device-vendor-select-label">Device Vendor</InputLabel>
                                <Select
                                    labelId="device-vendor-select-label"
                                    value={selectedDeviceVendor}
                                    onChange={(event) => handleSelectChange(setSelectedDeviceVendor, resetSelections)(event)}
                                    label="Device Vendor"
                                >
                                    {deviceVendors?.map((vendor, i) => (
                                        <MenuItem
                                            key={vendor}
                                            id="deviceVendor"
                                            value={vendor}
                                        >
                                            {vendor}
                                        </MenuItem>
                                    ))}
                                </Select>
                            </FormControl>
                        </Grid>
                        <Grid item xs={12} sm={6} md >
                            <FormControl variant="outlined" sx={{ width: '100%', height: '100%' }}>
                                <InputLabel id="device-network-select-label">Device Network</InputLabel>
                                <Select
                                    labelId="device-network-select-label"
                                    value={selectedDeviceNetwork}
                                    onChange={(event) => handleSelectChange(setSelectedDeviceNetwork, resetSelections)(event)}
                                    label="Device Network"
                                >
                                    {deviceNetworks?.map((network, i) => (
                                        <MenuItem
                                            key={network}
                                            id="deviceNetwork"
                                            value={network}
                                        >
                                            {network}
                                        </MenuItem>
                                    ))}
                                </Select>
                            </FormControl>
                        </Grid>
                        <Grid item xs={12} sm={6} md >
                            <Button variant="contained" color="primary" onClick={handleSubmit} disabled={!selectedDeviceName && !selectedDeviceIp && !selectedDeviceModel && !selectedDeviceVendor && !selectedDeviceNetwork}>
                                Submit
                            </Button>
                        </Grid>
                    </Grid>

                </CardContent>
            </Card>
            <Box mt={4} sx={{ width: "100%" }}>
                <DataGrid rows={data} columns={columns}
                    initialState={{
                        pagination: {
                            paginationModel: {
                                pageSize: 10,
                                page: 0
                            }
                        }
                    }}
                    sx={
                        {
                            boxShadow: 1,
                            border: 1,
                        }
                    }
                    pageSizeOptions={[5, 10, 25]}
                    pagination
                    slotProps={{
                        footer: {
                            sx: {
                                justifyContent: "flex-start"
                            }
                        }
                    }}
                    slots={{
                        toolbar: GridToolbar,
                    }}
                    checkboxSelection
                    disableRowSelectionOnClick
                    loading={loading} autoHeight />
            </Box>
        </Container>
    )
}

export default DashboardGridData
