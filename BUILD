java_binary(
		name = "tvrenamer",
		srcs = glob(["src/main/**/*.java"]),
		main_class = "com.google.code.tvrenamer.view.UIStarter",
		deps = [
				"lib/main/swt-osx64-4.3.jar",
				"lib/main/jedit-4.3.2-IOUtilities.jar",
				"@xstream//jar",
				"@xpp3//jar",
				"@commons-codec//jar",
		],
		resources = glob(["res/**"]),
		classpath_resources = ["src/main/tvrenamer.version"],
)

java_toolchain(
		name = "default",
		encoding = "UTF-8",
		source_version = "8",
		target_version = "8",
)
