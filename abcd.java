import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { __UCS_GRAPHQL_URL__ } from '../../api-endpoints';
import { AVAILABLE_ALARM_COLLECTION_TYPES_QUERY, FETCH_ALARM_METRICS_QUERY } from '../../graphQL/graphqlQueries';
import LoadingButton from "@mui/lab/LoadingButton";
import { LineChart } from '@mui/x-charts/LineChart';
import { AxisConfig, ChartsXAxisProps } from '@mui/x-charts';
import { Box, FormControl, Grid, InputLabel, MenuItem, Select, SelectChangeEvent, TextField, Button, Breadcrumbs, Link } from '@mui/material';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import dayjs, { Dayjs } from 'dayjs';
import { DatePicker } from "@mui/x-date-pickers/DatePicker";
import { useTheme } from "@mui/material/styles";
import { useSnackbar } from '../../utils/SnackbarContext';
import NavigateNextIcon from '@mui/icons-material/NavigateNext';
import HomeIcon from '@mui/icons-material/Home';

// Define types for the data
interface DataPoint {
  date: string;
  totalNumberOfFiles: number;
  totalSizeOfFilesBytes: number;
  trendsDrillDownList: DetailedDataPoint[];
}

interface DetailedDataPoint {
  time: string;
  sizeOfFilesBytes: number;
  numberOfFiles: number;
}

const chartHeight = 450;

const ParentChart = ({ data, onDrillDown }: { data: DataPoint[], onDrillDown: (date: string) => void }) => {
  const theme = useTheme();

  const transformedData = data.map(item => ({
    collectionDate: item.date,
    totalNumberOfFiles: item.totalNumberOfFiles,
    totalSizeOfFilesBytes: item.totalSizeOfFilesBytes,
  }));

  const lineChartParams = {
    series: [
      {
        id: 'totalNumberOfFiles',
        dataKey: 'totalNumberOfFiles',
        label: 'Number of Files',
        data: transformedData.map(item => item.totalNumberOfFiles),
      },
      {
        id: 'totalSizeOfFilesBytes',
        dataKey: 'totalSizeOfFilesBytes',
        label: 'Size of Files (Bytes)',
        data: transformedData.map(item => item.totalSizeOfFilesBytes),
      },
    ],
    xAxis: [{
      data: transformedData.map(item => item.collectionDate),
      id: 'axis1',
      dataKey: 'collectionDate',
      scaleType: "point",
      label: "Collected Date",
      tickLabelStyle: { angle: -25, textAnchor: 'end', fontSize: 10 },
      labelStyle: { transform: "translateY(30px)" },
    } as AxisConfig<'point', string, ChartsXAxisProps>],
    height: chartHeight,
    colors: theme.palette.primary.main,
  };

  return (
    <Box width="100%">
      <LineChart
        {...lineChartParams}
        onAxisClick={(event, d) => {
          if (d?.axisValue) {
            onDrillDown(String(d.axisValue));
          } else {
            console.error('Date not found in data point');
          }
        }}
      />
    </Box>
  );
};

const ChildChart = ({ date, data }: { date: string, data: DetailedDataPoint[] }) => {
  const theme = useTheme();
  const lineChartParams = {
    series: [
      {
        id: 'numberOfFiles',
        dataKey: 'numberOfFiles',
        label: 'Number of Files',
        data: data.map(item => item.numberOfFiles),
      },
      {
        id: 'sizeOfFilesBytes',
        dataKey: 'sizeOfFilesBytes',
        label: 'Size of Files (Bytes)',
        data: data.map(item => item.sizeOfFilesBytes),
      },
    ],
    xAxis: [{
      data: data.map(item => item.time),
      id: 'axis2',
      dataKey: 'time',
      scaleType: "point",
      label: `Details on ${date}`,
    } as AxisConfig<'point', string, ChartsXAxisProps>],
    height: chartHeight,
    colors: theme.palette.primary.main,
  };

  return (
    <Box width="100%">
      <LineChart {...lineChartParams} />
    </Box>
  );
};

const HealthMetricsDashboard: React.FC = () => {
  const theme = useTheme();
  const [collectionTypes, setCollectionTypes] = useState<any[]>([]);
  const [selectedCollectionType, setSelectedCollectionType] = useState<string>('');
  const [fromDate, setFromDate] = useState<Dayjs>(dayjs().subtract(1, 'month'));
  const [toDate, setToDate] = useState<Dayjs | null>(dayjs());
  const [loading, setLoading] = useState(false);
  const [responseData, setResponseData] = useState<DataPoint[]>([]);
  const [selectedDate, setSelectedDate] = useState<string | null>(null);

  const { showSnackbar } = useSnackbar();

  useEffect(() => {
    const fetchCollectionTypes = async () => {
      try {
        const response = await axios.post(__UCS_GRAPHQL_URL__, {
          query: AVAILABLE_ALARM_COLLECTION_TYPES_QUERY,
        });
        setCollectionTypes(response.data.data.availableAlarmCollectionTypes || []);
      } catch (error) {
        console.error('Error fetching collection types', error);
      }
    };
    fetchCollectionTypes();
  }, []);

  const handleFetch = async () => {
    setLoading(true);
    try {
      const response = await axios.post(__UCS_GRAPHQL_URL__, {
        query: FETCH_ALARM_METRICS_QUERY,
        variables: {
          collectionType: selectedCollectionType,
          fromDate: fromDate.format('YYYY-MM-DD'),
          toDate: toDate?.format('YYYY-MM-DD'),
        },
      });
      setResponseData(response.data.data.alarmMetrics || []);
    } catch (error) {
      console.error('Error fetching data', error);
      showSnackbar('Error fetching data');
    } finally {
      setLoading(false);
    }
  };

  const drillDownData = selectedDate
    ? responseData.find(item => item.date === selectedDate)?.trendsDrillDownList || []
    : [];

  return (
    <Box>
      {/* Controls */}
      <FormControl>
        <Select value={selectedCollectionType} onChange={(e) => setSelectedCollectionType(e.target.value)}>
          {collectionTypes.map(type => <MenuItem key={type.typeId} value={type.type}>{type.type}</MenuItem>)}
        </Select>
      </FormControl>
      <Button onClick={handleFetch}>Fetch</Button>

      {/* Parent Chart */}
      {!selectedDate && (
        <ParentChart data={responseData} onDrillDown={setSelectedDate} />
      )}

      {/* Child Chart */}
      {selectedDate && (
        <ChildChart date={selectedDate} data={drillDownData} />
      )}
    </Box>
  );
};

export default HealthMetricsDashboard;