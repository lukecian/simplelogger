package com.example.logutiltest;

interface ILogUploadListener
{
	void OnUploadBegin();
	void OnUploadComplete();
	void OnUploadError();
}