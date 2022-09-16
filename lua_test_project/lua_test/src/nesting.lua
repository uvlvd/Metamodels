bar = "hello"
baz = "world"
Foo = {
	bar = {
		baz = 42
	}
}
bar = "hello"
baz = "world"

-- This should resolve to 42
Foo.result = Foo.bar.baz

foo = Foo.result
bar = baz
