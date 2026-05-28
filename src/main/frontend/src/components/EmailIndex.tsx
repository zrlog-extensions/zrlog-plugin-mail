import {ReloadOutlined, SendOutlined, SettingOutlined} from "@ant-design/icons";
import {Line} from "@ant-design/plots";
import type {ColumnsType} from "antd/es/table";
import {
    Alert,
    Button,
    Descriptions,
    Drawer,
    Empty,
    Form,
    Input,
    InputNumber,
    Select,
    Space,
    Statistic,
    Table,
    Tag,
    Tooltip,
    Typography,
    message,
    theme,
} from "antd";
import axios from "axios";
import {FunctionComponent, useMemo, useState} from "react";
import {EmailConfig, EmailInfoResponse, EmailLogRow, EmailMetric, PageData, StandardResponse} from "../index";

type EmailIndexProps = {
    data: EmailInfoResponse;
}

type FilterValues = {
    keyword?: string;
    status?: string;
}

const retentionOptions = [
    {label: "30 天", value: 30},
    {label: "90 天", value: 90},
    {label: "180 天", value: 180},
];

const statusOptions = [
    {label: "全部状态", value: ""},
    {label: "成功", value: "success"},
    {label: "失败", value: "failed"},
];

const request = async <T, >(url: string, params?: Record<string, string>) => {
    const {data} = await axios.post<StandardResponse<T>>(url, new URLSearchParams(params), {
        headers: {"Content-Type": "application/x-www-form-urlencoded;charset=UTF-8"},
    });
    if (!data.success) {
        throw new Error(data.message || "操作失败");
    }
    return data.data;
};

const fetchLogs = async (params: Record<string, string>) => {
    const {data} = await axios.get<StandardResponse<PageData<EmailLogRow>>>("list", {params});
    if (!data.success) {
        throw new Error(data.message || "加载失败");
    }
    return data.data;
};

const statusColor = (status: EmailMetric["status"]) => {
    if (status === "processing") {
        return "processing";
    }
    if (status === "warning") {
        return "warning";
    }
    return "default";
};

const recipientsText = (recipients: string[]) => {
    if (!recipients || recipients.length === 0) {
        return "-";
    }
    return recipients.join(", ");
};

const EmailIndex: FunctionComponent<EmailIndexProps> = ({data}) => {
    const [config, setConfig] = useState<EmailConfig>(data.config);
    const [metrics, setMetrics] = useState<EmailMetric[]>(data.summary || []);
    const [trend, setTrend] = useState(data.trend || []);
    const [logs, setLogs] = useState<PageData<EmailLogRow>>(data.logs);
    const [filters, setFilters] = useState<FilterValues>({});
    const [loading, setLoading] = useState(false);
    const [settingOpen, setSettingOpen] = useState(false);
    const [detail, setDetail] = useState<EmailLogRow | null>(null);
    const [form] = Form.useForm<EmailConfig>();
    const [messageApi, contextHolder] = message.useMessage();
    const {token} = theme.useToken();

    const loadLogs = async (page = logs.page, pageSize = logs.pageSize, nextFilters = filters) => {
        setLoading(true);
        try {
            setLogs(await fetchLogs({
                page: String(page),
                pageSize: String(pageSize),
                keyword: nextFilters.keyword || "",
                status: nextFilters.status || "",
            }));
        } catch (e) {
            messageApi.error(e instanceof Error ? e.message : "加载失败");
        } finally {
            setLoading(false);
        }
    };

    const refreshPage = async () => {
        setLoading(true);
        try {
            const {data: response} = await axios.get<StandardResponse<EmailInfoResponse>>("json");
            if (!response.success) {
                throw new Error(response.message || "加载失败");
            }
            setConfig(response.data.config);
            setMetrics(response.data.summary || []);
            setTrend(response.data.trend || []);
            setLogs(response.data.logs);
        } catch (e) {
            messageApi.error(e instanceof Error ? e.message : "加载失败");
        } finally {
            setLoading(false);
        }
    };

    const openSetting = () => {
        form.setFieldsValue(config);
        setSettingOpen(true);
    };

    const saveSetting = async () => {
        const values = await form.validateFields();
        try {
            const saved = await request<EmailConfig>("update", {
                to: values.to || "",
                from: values.from || "",
                smtpServer: values.smtpServer || "",
                password: values.password || "",
                port: String(values.port || ""),
                emailLogRetentionDays: String(values.retentionDays || 30),
            });
            setConfig(saved);
            await refreshPage();
            messageApi.success("已保存");
            setSettingOpen(false);
        } catch (e) {
            messageApi.error(e instanceof Error ? e.message : "保存失败");
        }
    };

    const sendTest = async () => {
        setLoading(true);
        try {
            const result = await request<{ status: number }>("testEmailService");
            if (result.status === 200) {
                messageApi.success("测试邮件发送成功");
            } else {
                messageApi.error("测试邮件发送失败");
            }
            await refreshPage();
        } catch (e) {
            messageApi.error(e instanceof Error ? e.message : "测试邮件发送失败");
        } finally {
            setLoading(false);
        }
    };

    const columns = useMemo<ColumnsType<EmailLogRow>>(() => [
        {
            title: "发送时间",
            dataIndex: "time",
            width: 168,
        },
        {
            title: "主题",
            dataIndex: "subject",
            render: (value: string) => <Typography.Text strong ellipsis>{value || "-"}</Typography.Text>,
        },
        {
            title: "收件人",
            dataIndex: "recipients",
            render: (value: string[]) => (
                <Tooltip title={recipientsText(value)}>
                    <span>{recipientsText(value)}</span>
                </Tooltip>
            ),
        },
        {
            title: "来源",
            dataIndex: "source",
            width: 112,
            render: (value: string) => <Tag>{value || "服务调用"}</Tag>,
        },
        {
            title: "状态",
            dataIndex: "success",
            width: 96,
            render: (value: boolean) => <Tag color={value ? "success" : "error"}>{value ? "成功" : "失败"}</Tag>,
        },
        {
            title: "附件",
            dataIndex: "attachmentCount",
            width: 80,
        },
        {
            title: "详情",
            key: "action",
            width: 84,
            render: (_, row) => <Button size="small" onClick={() => setDetail(row)}>查看</Button>,
        },
    ], []);

    const emptyDescription = "暂无发送记录。发送测试邮件或被其他插件调用邮件服务后会显示记录。";

    return (
        <div className="email-shell">
            {contextHolder}
            <div className="email-topbar">
                <div>
                    <h1 className="email-title">邮件服务</h1>
                    <div className="email-subtitle">SMTP 配置和最近 {config.retentionDays || 30} 天发送记录</div>
                </div>
                <Space wrap>
                    <Button icon={<ReloadOutlined/>} onClick={refreshPage} loading={loading}>刷新</Button>
                    <Button icon={<SendOutlined/>} onClick={sendTest} loading={loading}>测试发送</Button>
                    <Button icon={<SettingOutlined/>} onClick={openSetting}>设置</Button>
                </Space>
            </div>

            <div className="summary-grid">
                {metrics.map(metric => (
                    <div className="summary-card" key={metric.label}>
                        <Statistic title={metric.label} value={metric.value}/>
                        <Tag className="summary-tag" color={statusColor(metric.status)}>{metric.status === "warning" ? "需关注" : "记录中"}</Tag>
                    </div>
                ))}
            </div>

            {logs.total === 0 && (
                <Alert
                    className="empty-guide"
                    type="info"
                    showIcon
                    message="还没有发送记录"
                    description={emptyDescription}
                    action={<Button size="small" onClick={sendTest}>测试发送</Button>}
                />
            )}

            <section className="trend-panel">
                <div className="panel-title">发送趋势</div>
                {trend.length === 0 ? (
                    <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无数据"/>
                ) : (
                    <Line
                        data={trend}
                        height={220}
                        autoFit
                        xField="date"
                        yField="value"
                        color={data.colorPrimary || token.colorPrimary}
                        point={{size: 3}}
                    />
                )}
            </section>

            <section className="log-panel">
                <div className="log-toolbar">
                    <Space wrap>
                        <Input.Search
                            allowClear
                            placeholder="搜索主题、收件人、错误"
                            onSearch={value => {
                                const next = {...filters, keyword: value};
                                setFilters(next);
                                loadLogs(1, logs.pageSize, next);
                            }}
                            style={{width: 260}}
                        />
                        <Select
                            value={filters.status || ""}
                            options={statusOptions}
                            onChange={value => {
                                const next = {...filters, status: value};
                                setFilters(next);
                                loadLogs(1, logs.pageSize, next);
                            }}
                            style={{width: 120}}
                        />
                    </Space>
                    <Typography.Text type="secondary">共 {logs.total} 条</Typography.Text>
                </div>
                <Table
                    rowKey="id"
                    size="middle"
                    loading={loading}
                    columns={columns}
                    dataSource={logs.rows}
                    scroll={{x: 980}}
                    locale={{emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={emptyDescription}/>}}
                    pagination={{
                        current: logs.page,
                        pageSize: logs.pageSize,
                        total: logs.total,
                        showSizeChanger: true,
                    }}
                    onChange={pagination => loadLogs(pagination.current || 1, pagination.pageSize || 10)}
                />
            </section>

            <Drawer
                title="邮件设置"
                open={settingOpen}
                width={460}
                onClose={() => setSettingOpen(false)}
                extra={<Button type="primary" onClick={saveSetting}>保存</Button>}
            >
                <Form form={form} layout="vertical">
                    <Form.Item label="发送邮箱地址" name="from" rules={[{required: true, message: "请输入发送邮箱地址"}]}>
                        <Input placeholder="sender@example.com"/>
                    </Form.Item>
                    <Form.Item label="SMTP 服务器" name="smtpServer" rules={[{required: true, message: "请输入 SMTP 服务器"}]}>
                        <Input placeholder="smtp.example.com"/>
                    </Form.Item>
                    <Form.Item label="密码" name="password" rules={[{required: true, message: "请输入密码"}]}>
                        <Input.Password/>
                    </Form.Item>
                    <Form.Item label="端口" name="port" rules={[{required: true, message: "请输入端口"}]}>
                        <InputNumber min={1} max={65535} style={{width: 140}}/>
                    </Form.Item>
                    <Form.Item label="默认收件邮箱" name="to" rules={[{required: true, message: "请输入默认收件邮箱"}]}>
                        <Input placeholder="receiver@example.com"/>
                    </Form.Item>
                    <Form.Item label="记录保留" name="retentionDays">
                        <Select options={retentionOptions}/>
                    </Form.Item>
                </Form>
            </Drawer>

            <Drawer
                title="发送详情"
                open={detail !== null}
                width={560}
                onClose={() => setDetail(null)}
            >
                {detail && (
                    <Descriptions column={1} size="small" bordered>
                        <Descriptions.Item label="发送时间">{detail.time}</Descriptions.Item>
                        <Descriptions.Item label="主题">{detail.subject || "-"}</Descriptions.Item>
                        <Descriptions.Item label="收件人">{recipientsText(detail.recipients)}</Descriptions.Item>
                        <Descriptions.Item label="来源">{detail.source || "-"}</Descriptions.Item>
                        <Descriptions.Item label="状态">
                            <Tag color={detail.success ? "success" : "error"}>{detail.success ? "成功" : "失败"}</Tag>
                        </Descriptions.Item>
                        <Descriptions.Item label="状态码">{detail.status || "-"}</Descriptions.Item>
                        <Descriptions.Item label="附件数">{detail.attachmentCount}</Descriptions.Item>
                        <Descriptions.Item label="错误">{detail.error || "-"}</Descriptions.Item>
                    </Descriptions>
                )}
            </Drawer>
        </div>
    );
};

export default EmailIndex;
