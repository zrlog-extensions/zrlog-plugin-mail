import axios from "axios";
import {useEffect, useState} from "react";
import {createRoot} from "react-dom/client";
import AppBase from "./AppBase";
import {PluginThemeProvider} from "./PluginThemeProvider";
import type {PluginAppearance} from "./PluginThemeProvider";

export interface Plugin {
    id: string;
    version: string;
    name: string;
    paths: string[];
    actions: string[];
    desc: string;
    author: string;
    shortName: string;
    indexPage: string;
    previewImageBase64: string;
    services: string[];
    dependentService: string[];
}

export interface EmailConfig {
    to: string;
    from: string;
    smtpServer: string;
    password: string;
    port: string;
    retentionDays: number;
}

export interface EmailMetric {
    label: string;
    value: number;
    status: "normal" | "processing" | "warning";
}

export interface EmailTrendRow {
    date: string;
    value: number;
}

export interface EmailLogRow {
    id: string;
    timestamp: number;
    time: string;
    recipients: string[];
    subject: string;
    source: string;
    success: boolean;
    status: number;
    error: string;
    attachmentCount: number;
}

export interface PageData<T> {
    rows: T[];
    total: number;
    page: number;
    pageSize: number;
}

export interface EmailInfoResponse extends PluginAppearance {
    plugin: Plugin;
    config: EmailConfig;
    summary: EmailMetric[];
    trend: EmailTrendRow[];
    logs: PageData<EmailLogRow>;
}

export interface StandardResponse<T> {
    success: boolean;
    message?: string;
    data: T;
}

const loadFromDocument = () => {
    try {
        const node = document.getElementById("pluginInfo");
        if (node === null || node.innerText.length === 0) {
            return null;
        }
        return JSON.parse(node.innerText) as StandardResponse<EmailInfoResponse>;
    } catch (e) {
        return null;
    }
};

const Index = () => {
    const [response, setResponse] = useState<StandardResponse<EmailInfoResponse> | null>(loadFromDocument);

    useEffect(() => {
        if (response === null) {
            axios.get<StandardResponse<EmailInfoResponse>>("json").then(({data}) => {
                setResponse(data);
            });
        }
    }, [response]);

    if (response === null || !response.success) {
        return <></>;
    }

    return (
        <PluginThemeProvider page={response.data}>
            <AppBase pluginInfo={response.data}/>
        </PluginThemeProvider>
    );
};

const container = document.getElementById("app");
const root = createRoot(container!);
root.render(<Index/>);
